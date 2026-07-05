package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.queue.MessageQueueDto;
import com.example.smartmessaging.dto.request.KakaoFeedMessageRequest;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.mapper.SendMapper;
import com.example.smartmessaging.service.CampaignDraftService;
import com.example.smartmessaging.service.SendQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendQueueServiceImpl implements SendQueueService {

    private final SendMapper sendMapper;
    private final RabbitTemplate rabbitTemplate;
    private final CampaignDraftService draftService;
    private final com.example.smartmessaging.mapper.CustomerMapper customerMapper;

    @Override
    @Transactional
    public void queueKakaoMessages(Long userId, String creator, KakaoFeedMessageRequest request) {
        List<String> targetUuids = request.getTargetUuids();
        if (targetUuids == null || targetUuids.isEmpty()) {
            throw new IllegalArgumentException("수신 대상(UUID)이 비어 있습니다.");
        }

        log.info("📢 [SendQueue] 발송 작업 대기열 등록 시작 - 마스터 생성 (대상: {} 건)", targetUuids.size());

        // 1. 발송 마스터 이력(send_history) DB 생성
        SendHistoryVO history = SendHistoryVO.builder()
                .templateId(null) // 수동 작성 템플릿 처리
                .userId(userId)
                .title(request.getTitle())
                .content(request.getDescription())
                .purpose("INFO")
                .totalTargetCount(targetUuids.size())
                .successCount(0)
                .failCount(0)
                .build();
        history.setCreatedBy(userId);
        sendMapper.insertSendHistory(history);
        Long sendHistoryId = history.getId();

        // 2. 개별 발송 대상(send_target) DB 적재 및 RabbitMQ 대기열 Enqueue
        for (String uuid : targetUuids) {
            // 2-1. 개별 타겟 레코드 생성
            SendTargetVO target = SendTargetVO.builder()
                    .sendHistoryId(sendHistoryId)
                    .customerId(null) // 카카오 친구 목록 발송 시 customerId 매핑 생략
                    .finalChannelId(2L) // 카카오톡 발송 채널 ID 하드코딩 매핑 (기본 카카오 채널 ID=2 가정)
                    .cost(BigDecimal.valueOf(15.0)) // 카카오 알림톡 기본 가상 발송 비용 15원
                    .userUuid(uuid)
                    .status("PENDING")
                    .build();
            target.setCreatedBy(userId);
            sendMapper.insertSendTarget(target);
            Long sendTargetId = target.getId();

            // 2-2. 큐 전송용 DTO 패키징
            MessageQueueDto queueMessage = MessageQueueDto.builder()
                    .sendTargetId(sendTargetId)
                    .customerId(null)
                    .recipientNo(uuid) // 카카오 친구 발송 시 수신 식별자는 UUID
                    .channelType("KAKAO")
                    .title(request.getTitle())
                    .content(request.getDescription())
                    .build();

            // 2-3. RabbitMQ 메인 큐로 전송 (고속 인큐)
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MAIN_EXCHANGE,
                    RabbitMQConfig.MAIN_ROUTING_KEY,
                    queueMessage
            );
        }

        log.info("🎉 [SendQueue] 발송 작업 {} 건 대기열 큐 적재 성공 - HistoryID: {}", targetUuids.size(), sendHistoryId);
    }

    @Override
    @Transactional
    public void queueCampaignMessages(Long userId, String draftId, com.example.smartmessaging.dto.request.MessageSendRequestDTO request) {
        if (draftId == null || draftId.isBlank()) {
            throw new IllegalArgumentException("Draft ID가 유효하지 않습니다.");
        }

        // 1. Redis에서 총 수신자 수만 우선 파악 (메모리 로드 방지, 초경량)
        long totalCount = draftService.getTotalCount(userId, draftId);
        if (totalCount == 0) {
            throw new IllegalArgumentException("발송 대상 수신자가 없습니다.");
        }

        log.info("📢 [SendQueue] 캠페인 초경량 비동기 위임 시작 - draftId={}, 총 대상: {} 명", draftId, totalCount);

        // 2. 발송 마스터 이력(send_history) 생성
        SendHistoryVO history = SendHistoryVO.builder()
                .templateId(request.getTemplateId())
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .purpose(request.getPurpose())
                .totalTargetCount((int) totalCount)
                .successCount(0)
                .failCount(0)
                .build();
        history.setCreatedBy(userId);
        sendMapper.insertSendHistory(history);
        Long sendHistoryId = history.getId();

        // 3. 백그라운드 위임을 위한 초경량 명령 DTO 패키징
        com.example.smartmessaging.dto.queue.CampaignCommandQueueDto command = com.example.smartmessaging.dto.queue.CampaignCommandQueueDto.builder()
                .sendHistoryId(sendHistoryId)
                .draftId(draftId)
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .purpose(request.getPurpose())
                .templateId(request.getTemplateId())
                .routingChannelIds(request.getRoutingChannelIds())
                .build();

        // 4. RabbitMQ 캠페인 전송 명령 전용 큐로 1건 고속 인큐 (0.1ms 소요)
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CAMP_COMMAND_EXCHANGE,
                RabbitMQConfig.CAMP_COMMAND_ROUTING_KEY,
                command
        );

        log.info("🎉 [SendQueue] 캠페인 초경량 명령 큐 등록 성공 - HistoryID: {}, DraftID: {}", sendHistoryId, draftId);
    }
}

