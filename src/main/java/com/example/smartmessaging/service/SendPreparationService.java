package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.model.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.queue.CampaignCommandQueueDto;
import com.example.smartmessaging.dto.request.SendPrepareRequestDTO;
import com.example.smartmessaging.dto.response.CostEstimationResponseDTO;
import com.example.smartmessaging.dto.response.SendPrepareResponseDTO;
import com.example.smartmessaging.dto.vo.SendHistoryRoutingVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.service.queue.MessageQueuePublisher;
import com.example.smartmessaging.service.repository.SendQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SendPreparationService {

    private static final int ORACLE_IN_CHUNK_SIZE = 900;
    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";

    private final CampaignDraftService draftService;
    private final SendQueueRepository sendQueueRepository;
    private final RecipientChannelResolver recipientChannelResolver;
    private final MessageQueuePublisher messageQueuePublisher;

    @Value("${messaging.advertising.quiet-start-hour:21}")
    private int advertisingQuietStartHour;

    @Value("${messaging.advertising.quiet-end-hour:8}")
    private int advertisingQuietEndHour;

    @Transactional(readOnly = true)
    public CostEstimationResponseDTO estimate(Long userId, SendPrepareRequestDTO request) {
        List<Long> customerIds = draftService.getAllIds(userId, request.getDraftId());
        LocalDateTime advertisingSendAt = resolveEffectiveScheduledAt(request);
        LocalDateTime displaySendAt = shouldDisplayScheduledAt(request) ? advertisingSendAt : null;
        boolean restricted = Boolean.TRUE.equals(request.getAdvertising())
                && advertisingSendAt.isAfter(requestedOrNow(request).plusSeconds(1));

        if (customerIds.isEmpty()) {
            return CostEstimationResponseDTO.builder()
                    .totalTargetCount(0)
                    .sendableTargetCount(0)
                    .skippedTargetCount(0)
                    .estimatedCost(BigDecimal.ZERO)
                    .advertisingRestricted(restricted)
                    .advertisingSendAt(displaySendAt)
                    .channels(List.of())
                    .build();
        }

        List<SendRecipientCandidateVO> candidates = findCandidates(customerIds);
        List<RecipientSendPlan> plans = recipientChannelResolver.resolve(
                customerIds,
                candidates,
                request.getChannelPriorityIds(),
                request.getContent());

        return toEstimationResponse(plans, displaySendAt, restricted);
    }

    @Transactional
    public SendPrepareResponseDTO queueCampaign(Long userId, SendPrepareRequestDTO request) {
        CostEstimationResponseDTO estimate = estimate(userId, request);
        LocalDateTime effectiveScheduledAt = resolveEffectiveScheduledAt(request);
        boolean delayed = effectiveScheduledAt.isAfter(LocalDateTime.now().plusSeconds(1));
        String status = delayed ? STATUS_SCHEDULED : STATUS_QUEUED;

        SendHistoryVO history = SendHistoryVO.builder()
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .purpose(resolvePurpose(request))
                .status(status)
                .totalTargetCount(estimate.getTotalTargetCount())
                .successCount(0)
                .failCount(0)
                .estimatedCost(estimate.getEstimatedCost())
                .estimatedSaving(BigDecimal.ZERO)
                .actualCost(BigDecimal.ZERO)
                .scheduledAt(effectiveScheduledAt)
                .build();
        history.setCreatedBy(userId);
        history.setUpdatedBy(userId);
        sendQueueRepository.insertSendHistory(history);

        for (int i = 0; i < request.getChannelPriorityIds().size(); i++) {
            SendHistoryRoutingVO routing = SendHistoryRoutingVO.builder()
                    .sendHistoryId(history.getId())
                    .channelId(request.getChannelPriorityIds().get(i))
                    .priorityOrder(i + 1)
                    .build();
            routing.setCreatedBy(userId);
            routing.setUpdatedBy(userId);
            sendQueueRepository.insertSendHistoryRouting(routing);
        }

        messageQueuePublisher.publishCampaignCommandAfterCommit(CampaignCommandQueueDto.builder()
                .sendHistoryId(history.getId())
                .userId(userId)
                .draftId(request.getDraftId())
                .title(request.getTitle())
                .content(request.getContent())
                .linkUrl(request.getLinkUrl())
                .advertising(Boolean.TRUE.equals(request.getAdvertising()))
                .scheduledAt(effectiveScheduledAt)
                .build());

        return SendPrepareResponseDTO.builder()
                .sendHistoryId(history.getId())
                .status(status)
                .queued(true)
                .scheduledAt(effectiveScheduledAt)
                .message(delayed ? "Campaign scheduled and queued for preparation." : "Campaign queued for preparation.")
                .build();
    }

    private CostEstimationResponseDTO toEstimationResponse(
            List<RecipientSendPlan> plans,
            LocalDateTime advertisingSendAt,
            boolean restricted) {
        Map<Long, CostEstimationResponseDTO.ChannelEstimate> channelMap = new LinkedHashMap<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        int sendableCount = 0;

        for (RecipientSendPlan plan : plans) {
            if (!plan.isSendable()) {
                continue;
            }
            sendableCount++;
            BigDecimal cost = plan.getEstimatedCost() == null ? BigDecimal.ZERO : plan.getEstimatedCost();
            totalCost = totalCost.add(cost);

            CostEstimationResponseDTO.ChannelEstimate row = channelMap.computeIfAbsent(plan.getFirstChannelId(),
                    channelId -> CostEstimationResponseDTO.ChannelEstimate.builder()
                            .channelId(channelId)
                            .channelType(plan.getFirstChannelType())
                            .targetCount(0)
                            .estimatedCost(BigDecimal.ZERO)
                            .build());
            row.setTargetCount(row.getTargetCount() + 1);
            row.setEstimatedCost(row.getEstimatedCost().add(cost));
        }

        List<CostEstimationResponseDTO.ChannelEstimate> channelEstimates = channelMap.values().stream()
                .sorted(Comparator.comparing(CostEstimationResponseDTO.ChannelEstimate::getChannelId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        return CostEstimationResponseDTO.builder()
                .totalTargetCount(plans.size())
                .sendableTargetCount(sendableCount)
                .skippedTargetCount(plans.size() - sendableCount)
                .estimatedCost(totalCost)
                .advertisingRestricted(restricted)
                .advertisingSendAt(advertisingSendAt)
                .channels(channelEstimates)
                .build();
    }

    private List<SendRecipientCandidateVO> findCandidates(List<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<SendRecipientCandidateVO> candidates = new java.util.ArrayList<>();
        for (int start = 0; start < customerIds.size(); start += ORACLE_IN_CHUNK_SIZE) {
            int end = Math.min(start + ORACLE_IN_CHUNK_SIZE, customerIds.size());
            candidates.addAll(sendQueueRepository.findRecipientCandidatesByCustomerIds(customerIds.subList(start, end)));
        }
        return candidates;
    }

    private String resolvePurpose(SendPrepareRequestDTO request) {
        if (request.getPurpose() != null && !request.getPurpose().isBlank()) {
            return request.getPurpose();
        }
        return Boolean.TRUE.equals(request.getAdvertising()) ? "AD" : "INFO";
    }

    private LocalDateTime resolveEffectiveScheduledAt(SendPrepareRequestDTO request) {
        LocalDateTime requestedAt = requestedOrNow(request);
        if (!Boolean.TRUE.equals(request.getAdvertising())) {
            return requestedAt;
        }
        return adjustAdvertisingTime(requestedAt);
    }

    private boolean shouldDisplayScheduledAt(SendPrepareRequestDTO request) {
        return request.getScheduledAt() != null || Boolean.TRUE.equals(request.getAdvertising());
    }

    private LocalDateTime requestedOrNow(SendPrepareRequestDTO request) {
        return Objects.requireNonNullElseGet(request.getScheduledAt(), LocalDateTime::now);
    }

    private LocalDateTime adjustAdvertisingTime(LocalDateTime requestedAt) {
        int hour = requestedAt.getHour();
        if (isAdvertisingQuietHour(hour)) {
            if (hour >= advertisingQuietStartHour) {
                return requestedAt.plusDays(1).withHour(advertisingQuietEndHour).withMinute(0).withSecond(0).withNano(0);
            }
            return requestedAt.withHour(advertisingQuietEndHour).withMinute(0).withSecond(0).withNano(0);
        }
        return requestedAt;
    }

    private boolean isAdvertisingQuietHour(int hour) {
        if (advertisingQuietStartHour < advertisingQuietEndHour) {
            return hour >= advertisingQuietStartHour && hour < advertisingQuietEndHour;
        }
        return hour >= advertisingQuietStartHour || hour < advertisingQuietEndHour;
    }
}
