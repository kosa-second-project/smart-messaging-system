package com.example.smartmessaging.service;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.queue.CampaignCommandQueueDto;
import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.mapper.SendPreparationMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignCommandConsumer {

    private static final int CHUNK_SIZE = 1000;

    private final CampaignDraftService draftService;
    private final ChannelService channelService;
    private final SendPreparationMapper sendPreparationMapper;
    private final RecipientChannelResolver recipientChannelResolver;
    private final ShortUrlService shortUrlService;
    private final MessageQueuePublisher messageQueuePublisher;
    private final com.example.smartmessaging.mapper.SendMapper sendMapper;

    /**
     * 캠페인 초경량 명령 대기열(campaign.command.queue)에서 메시지를 꺼내와 
     * 백그라운드 스레드에서 수신자별 라우팅 분석, DB 적재, 단축 URL 변환 및 메인 발송 큐 발행을 완수합니다.
     */
    @RabbitListener(queues = RabbitMQConfig.CAMP_COMMAND_QUEUE)
    @Transactional
    public void consumeCampaignCommand(CampaignCommandQueueDto command, Channel rabbitChannel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("📥 [Campaign Consumer] 백그라운드 발송 전개 시작 - HistoryID: {}, DraftID: {}", command.getSendHistoryId(), command.getDraftId());

        try {
            // 0. 마스터 캠페인 상태 확인 (예약 대기 상태인지 판별)
            SendHistoryVO campaign = sendPreparationMapper.selectSendHistoryById(command.getSendHistoryId());
            boolean isScheduled = (campaign != null && "SCHEDULED".equals(campaign.getStatus()));

            // 1. Redis에서 수신 대상 ID 리스트 복원
            List<Long> customerIds = draftService.getDraftCustomerIds(command.getUserId(), command.getDraftId());
            if (customerIds == null || customerIds.isEmpty()) {
                throw new IllegalStateException("수신 대상 목록이 비어 있거나 이미 만료되었습니다.");
            }

            // 2. 활성 채널 목록 및 수신자 후보 정보 조회
            List<ChannelVO> activeChannels = channelService.getActiveChannels();
            List<SendRecipientCandidateVO> recipients = findRecipientCandidates(customerIds);

            // 우선순위 채널 타입명 복원
            List<String> priorities = new ArrayList<>();
            if (command.getRoutingChannelIds() != null) {
                priorities = command.getRoutingChannelIds().stream()
                        .map(id -> activeChannels.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null))
                        .filter(Objects::nonNull)
                        .map(ChannelVO::getChannelType)
                        .toList();
            }

            // 3. 최적 발송 계획 연산 (RecipientChannelResolver 위임)
            List<RecipientSendPlan> plans = recipientChannelResolver.resolve(recipients, activeChannels, priorities);
            if (plans == null || plans.isEmpty()) {
                throw new IllegalStateException("발송 가능한 수신자 계획이 존재하지 않습니다.");
            }

            // 4. 수신자별로 발송 준비 진행 (DB 저장 및 단축 링크 / 080 수신거부 치환 후 Enqueue)
            int published = 0;
            for (RecipientSendPlan plan : plans) {
                // target 이력 적재
                SendTargetVO target = SendTargetVO.builder()
                        .sendHistoryId(command.getSendHistoryId())
                        .customerId(plan.getCustomerId())
                        .finalChannelId(plan.getFirstChannelId())
                        .status("PENDING")
                        .cost(plan.getEstimatedCost())
                        .userUuid(plan.getRecipient().getKakaoUserKey())
                        .build();
                target.setCreatedBy(command.getUserId());
                sendPreparationMapper.insertSendTarget(target);

                // 발송용 명령 Task 조립
                MessageTaskDto task = buildTask(command, target, plan);
                
                // 즉시 발송 상태일 경우에만 메인 발송 대기열로 Enqueue
                if (!isScheduled) {
                    messageQueuePublisher.publish(task);
                    published++;
                }
            }

            // 5. 완료 후 Redis 드래프트 임시데이터 안전 제거
            draftService.deleteDraft(command.getUserId(), command.getDraftId());

            log.info("🎉 [Campaign Consumer] 백그라운드 발송 전개 완료 - HistoryID: {}, 총 적재 건수: {} 건, 즉시 발송 처리 건수: {} 건", 
                    command.getSendHistoryId(), plans.size(), published);
            
            // 수동 ACK: 큐에서 메시지 제거
            rabbitChannel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("❌ [Campaign Consumer] 백그라운드 처리 실패 - HistoryID: {}, Error: {}", command.getSendHistoryId(), e.getMessage(), e);
            // 실패 시 Dead Letter Queue로 이송하여 격리
            rabbitChannel.basicNack(deliveryTag, false, false);
        }
    }

    private List<SendRecipientCandidateVO> findRecipientCandidates(List<Long> customerIds) {
        if (customerIds.size() <= CHUNK_SIZE) {
            return sendPreparationMapper.findRecipientCandidatesByCustomerIds(customerIds);
        }

        List<SendRecipientCandidateVO> candidates = new ArrayList<>();
        for (int i = 0; i < customerIds.size(); i += CHUNK_SIZE) {
            List<Long> chunk = customerIds.subList(i, Math.min(customerIds.size(), i + CHUNK_SIZE));
            candidates.addAll(sendPreparationMapper.findRecipientCandidatesByCustomerIds(chunk));
        }
        return candidates;
    }

    private MessageTaskDto buildTask(CampaignCommandQueueDto command, SendTargetVO target, RecipientSendPlan plan) {
        // 단축 URL 생성 (가상 목적 CLICK 지정)
        String actionUrl = null;
        // 본문 내에 http/https 링크가 있다면 추출해서 단축 URL 연동 (간단 매핑)
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("https?://[^\\s]+").matcher(command.getContent());
        if (matcher.find()) {
            String originalUrl = matcher.group();
            actionUrl = shortUrlService.createTrackedUrl(target.getId(), originalUrl.trim(), ShortUrlPurpose.CLICK);
        }

        // 광고성일 시 080 수신거부 링크 자동 변환
        String unsubscribeUrl = shouldCreateUnsubscribeUrl(command, plan)
                ? shortUrlService.createTrackedUrl(target.getId(), null, ShortUrlPurpose.UNSUBSCRIBE)
                : null;

        return MessageTaskDto.builder()
                .messageId("send-" + command.getSendHistoryId() + "-" + target.getId())
                .sendHistoryId(command.getSendHistoryId())
                .sendTargetId(target.getId())
                .templateId(command.getTemplateId())
                .campaignId(command.getSendHistoryId())
                .customerId(plan.getCustomerId())
                .phoneNumber(plan.getRecipient().getPhone())
                .email(plan.getRecipient().getEmail())
                .kakaoUserKey(plan.getRecipient().getKakaoUserKey())
                .title(command.getTitle())
                .content(command.getContent())
                .purpose(command.getPurpose())
                .actionButtonName("자세히 보기")
                .actionUrl(actionUrl)
                .unsubscribeUrl(unsubscribeUrl)
                .fallbackSequence(plan.getFallbackSequence())
                .currentStep(0)
                .build();
    }

    private boolean shouldCreateUnsubscribeUrl(CampaignCommandQueueDto command, RecipientSendPlan plan) {
        if (!"AD".equals(command.getPurpose().trim().toUpperCase(Locale.ROOT))) {
            return false;
        }
        return plan.getFallbackSequence() != null
                && plan.getFallbackSequence().stream().map(this::normalizeChannelType).anyMatch(channel -> "SMS".equals(channel) || "LMS".equals(channel));
    }

    private String normalizeChannelType(String channelType) {
        if (channelType == null) {
            return "";
        }
        String normalized = channelType.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("KAKAO")) {
            return "KAKAO";
        }
        return normalized;
    }
}
