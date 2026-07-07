package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.queue.CampaignCommandQueueDto;
import com.example.smartmessaging.dto.request.KakaoFeedMessageRequest;
import com.example.smartmessaging.dto.request.MessageSendRequestDTO;
import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.service.CampaignDraftService;
import com.example.smartmessaging.service.SendQueueService;
import com.example.smartmessaging.service.repository.SendMapper;
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

    @Override
    @Transactional
    public void queueKakaoMessages(Long userId, String creator, KakaoFeedMessageRequest request, String kakaoAccessToken) {
        List<String> targetUuids = request.getTargetUuids();
        if (targetUuids == null || targetUuids.isEmpty()) {
            throw new IllegalArgumentException("Target UUID list is empty.");
        }

        SendHistoryVO history = SendHistoryVO.builder()
                .templateId(null)
                .userId(userId)
                .title(request.getTitle())
                .content(request.getDescription())
                .purpose("INFO")
                .status("SENDING")
                .totalTargetCount(targetUuids.size())
                .successCount(0)
                .failCount(0)
                .estimatedCost(BigDecimal.ZERO)
                .estimatedSaving(BigDecimal.ZERO)
                .actualCost(BigDecimal.ZERO)
                .build();
        history.setCreatedBy(userId);
        sendMapper.insertSendHistory(history);

        for (String uuid : targetUuids) {
            SendTargetVO target = SendTargetVO.builder()
                    .sendHistoryId(history.getId())
                    .customerId(null)
                    .finalChannelId(2L)
                    .cost(BigDecimal.ZERO)
                    .userUuid(uuid)
                    .status("PENDING")
                    .build();
            target.setCreatedBy(userId);
            sendMapper.insertSendTarget(target);

            MessageTaskDto task = MessageTaskDto.builder()
                    .messageId("send-" + history.getId() + "-" + target.getId())
                    .sendHistoryId(history.getId())
                    .sendTargetId(target.getId())
                    .customerId(null)
                    .isRealCustomer(true)
                    .userId(userId)
                    .kakaoUserKey(uuid)
                    .kakaoAccessToken(kakaoAccessToken)
                    .title(request.getTitle())
                    .content(request.getDescription())
                    .purpose("INFO")
                    .fallbackSequence(List.of("KAKAO"))
                    .currentStep(0)
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MAIN_EXCHANGE,
                    RabbitMQConfig.MAIN_ROUTING_KEY,
                    task
            );
        }

        log.info("[SendQueue] queued kakao messages sendHistoryId={}, count={}", history.getId(), targetUuids.size());
    }

    @Override
    @Transactional
    public void queueCampaignMessages(Long userId, String draftId, MessageSendRequestDTO request, String kakaoAccessToken) {
        if (draftId == null || draftId.isBlank()) {
            throw new IllegalArgumentException("Draft ID is required.");
        }

        long totalCount = draftService.getTotalCount(userId, draftId);
        if (totalCount == 0) {
            throw new IllegalArgumentException("No recipients selected.");
        }

        SendHistoryVO history = SendHistoryVO.builder()
                .templateId(request.getTemplateId())
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .purpose(request.getPurpose())
                .status("SENDING")
                .totalTargetCount((int) totalCount)
                .successCount(0)
                .failCount(0)
                .estimatedCost(BigDecimal.ZERO)
                .estimatedSaving(BigDecimal.ZERO)
                .actualCost(BigDecimal.ZERO)
                .build();
        history.setCreatedBy(userId);
        sendMapper.insertSendHistory(history);

        CampaignCommandQueueDto command = CampaignCommandQueueDto.builder()
                .sendHistoryId(history.getId())
                .draftId(draftId)
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .purpose(request.getPurpose())
                .templateId(request.getTemplateId())
                .routingChannelIds(request.getRoutingChannelIds())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CAMP_COMMAND_EXCHANGE,
                RabbitMQConfig.CAMP_COMMAND_ROUTING_KEY,
                command
        );

        log.info("[SendQueue] queued campaign command sendHistoryId={}, draftId={}", history.getId(), draftId);
    }
}
