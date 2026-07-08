package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.response.HistoryChannelResponseDTO;
import com.example.smartmessaging.dto.response.HistoryDetailResponseDTO;
import com.example.smartmessaging.dto.response.HistoryFilterOptionDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.HistoryRetryFailedResponseDTO;
import com.example.smartmessaging.dto.response.HistoryStatusOptionDTO;
import com.example.smartmessaging.dto.response.HistoryTagResponseDTO;
import com.example.smartmessaging.dto.response.PageResponseDTO;
import com.example.smartmessaging.dto.type.SendHistoryStatus;
import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.example.smartmessaging.service.ChannelService;
import com.example.smartmessaging.service.HistoryService;
import com.example.smartmessaging.service.RecipientChannelResolver;
import com.example.smartmessaging.service.ShortUrlService;
import com.example.smartmessaging.service.TokenCryptoService;
import com.example.smartmessaging.service.queue.MessageQueuePublisher;
import com.example.smartmessaging.service.repository.HistoryMapper;
import com.example.smartmessaging.service.repository.SendPreparationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryServiceImpl implements HistoryService {
    private static final int RETRY_CHUNK_SIZE = 1000;

    private final HistoryMapper historyMapper;
    private final SendPreparationMapper sendPreparationMapper;
    private final ChannelService channelService;
    private final RecipientChannelResolver recipientChannelResolver;
    private final ShortUrlService shortUrlService;
    private final MessageQueuePublisher messageQueuePublisher;
    private final TokenCryptoService tokenCryptoService;

    @Override
    public PageResponseDTO<HistoryListResponseDTO> getHistories(HistorySearchRequestDTO condition) {
        condition = condition.normalized();
        long totalElements = historyMapper.countHistories(condition);
        int totalPages = totalElements == 0 ? 0
                : (int) Math.ceil((double) totalElements / condition.pageSize());

        if (totalPages > 0 && condition.page() > totalPages) {
            condition = condition.withPage(totalPages);
        }

        List<HistoryListResponseDTO> histories = totalElements == 0
                ? Collections.emptyList()
                : historyMapper.findHistories(condition);
        histories = attachChannelsAndTags(histories);

        return PageResponseDTO.of(histories, condition.page(), HistorySearchRequestDTO.PAGE_SIZE, totalElements);
    }

    @Override
    public HistoryDetailResponseDTO getHistoryDetail(Long sendHistoryId) {
        HistoryDetailResponseDTO detail = historyMapper.findHistoryDetailById(sendHistoryId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.SEND_HISTORY_NOT_FOUND);
        }

        List<Long> historyIds = List.of(sendHistoryId);
        detail = detail.withChannels(historyMapper.findChannelsByHistoryIds(historyIds).stream()
                .map(HistoryChannelResponseDTO::channelName)
                .toList());
        detail = detail.withTags(historyMapper.findTagsByHistoryIds(historyIds).stream()
                .map(HistoryTagResponseDTO::tagName)
                .toList());
        detail = detail.withAttemptFlows(historyMapper.findAttemptFlowsByHistoryId(sendHistoryId));
        int retryableFailCount = calculateRetryableFailCount(sendHistoryId, detail.failCount());
        int failCount = detail.failCount() == null ? 0 : detail.failCount();
        return detail.withRetryAvailability(retryableFailCount, Math.max(failCount - retryableFailCount, 0));
    }

    private int calculateRetryableFailCount(Long sendHistoryId, Integer failCount) {
        if (failCount == null || failCount <= 0) {
            return 0;
        }

        List<SendTargetVO> failedTargets = historyMapper.findFailedTargetsForRetry(sendHistoryId, failCount);
        if (failedTargets.isEmpty()) {
            return 0;
        }

        List<ChannelVO> activeChannels = channelService.getActiveChannels();
        List<Long> routingChannelIds = sendPreparationMapper.findRoutingChannelIdsByHistoryId(sendHistoryId);
        List<String> priorities = restorePriorities(routingChannelIds, activeChannels);
        List<Long> customerIds = failedTargets.stream().map(SendTargetVO::getCustomerId).toList();
        List<SendRecipientCandidateVO> recipients = sendPreparationMapper.findRecipientCandidatesByCustomerIds(customerIds);
        List<RecipientSendPlan> plans = recipientChannelResolver.resolve(recipients, activeChannels, priorities);
        if (plans == null || plans.isEmpty()) {
            return 0;
        }

        Set<Long> retryableCustomerIds = plans.stream()
                .map(RecipientSendPlan::getCustomerId)
                .collect(Collectors.toSet());
        return (int) failedTargets.stream()
                .filter(target -> retryableCustomerIds.contains(target.getCustomerId()))
                .count();
    }
    @Override
    @Transactional
    public HistoryRetryFailedResponseDTO retryFailedTargets(Long sendHistoryId) {
        SendHistoryVO campaign = sendPreparationMapper.selectSendHistoryById(sendHistoryId);
        if (campaign == null) {
            throw new BusinessException(ErrorCode.SEND_HISTORY_NOT_FOUND);
        }

        List<ChannelVO> activeChannels = channelService.getActiveChannels();
        List<Long> routingChannelIds = sendPreparationMapper.findRoutingChannelIdsByHistoryId(sendHistoryId);
        List<String> priorities = restorePriorities(routingChannelIds, activeChannels);
        String kakaoAccessToken = decryptKakaoTokenOrNull(campaign);

        int retryTargetCount = 0;
        int publishedCount = 0;
        int skippedCount = 0;
        Set<Long> skippedTargetIds = new HashSet<>();

        while (true) {
            List<SendTargetVO> targets = historyMapper.findFailedTargetsForRetry(sendHistoryId, RETRY_CHUNK_SIZE);
            if (targets.isEmpty()) {
                break;
            }

            List<SendTargetVO> retryCandidates = targets.stream()
                    .filter(target -> !skippedTargetIds.contains(target.getId()))
                    .toList();
            if (retryCandidates.isEmpty()) {
                break;
            }
            retryTargetCount += retryCandidates.size();

            List<Long> customerIds = retryCandidates.stream().map(SendTargetVO::getCustomerId).toList();
            List<SendRecipientCandidateVO> recipients = sendPreparationMapper.findRecipientCandidatesByCustomerIds(customerIds);
            List<RecipientSendPlan> plans = recipientChannelResolver.resolve(recipients, activeChannels, priorities);
            if (plans == null) {
                plans = List.of();
            }

            Map<Long, RecipientSendPlan> planByCustomerId = plans.stream()
                    .collect(Collectors.toMap(RecipientSendPlan::getCustomerId, Function.identity(), (left, right) -> left));

            List<SendTargetVO> publishableTargets = new ArrayList<>();
            for (SendTargetVO target : retryCandidates) {
                RecipientSendPlan plan = planByCustomerId.get(target.getCustomerId());
                if (plan == null) {
                    skippedTargetIds.add(target.getId());
                    skippedCount++;
                    continue;
                }
                publishableTargets.add(target);
            }

            if (publishableTargets.isEmpty()) {
                continue;
            }

            List<Long> publishedTargetIds = publishableTargets.stream().map(SendTargetVO::getId).toList();
            historyMapper.updateSendTargetStatusByIds(publishedTargetIds, "PENDING");
            historyMapper.decrementFailCount(sendHistoryId, publishedTargetIds.size());
            sendPreparationMapper.updateHistoryStatus(sendHistoryId, "SENDING");

            for (SendTargetVO target : publishableTargets) {
                RecipientSendPlan plan = planByCustomerId.get(target.getCustomerId());
                messageQueuePublisher.publish(buildRetryTask(campaign, kakaoAccessToken, target, plan));
                publishedCount++;
            }
        }

        if (retryTargetCount == 0) {
            return retryResponse(sendHistoryId, 0, 0, 0, "재발송할 실패 대상자가 없습니다.");
        }
        if (publishedCount == 0 && historyMapper.countUnfinishedTargets(sendHistoryId) == 0) {
            historyMapper.finalizeSendHistory(sendHistoryId, "SENT");
        }

        String message = publishedCount + "건을 재발송 큐에 등록했습니다.";
        if (skippedCount > 0) {
            message += " " + skippedCount + "건은 현재 수신 가능한 채널이 없어 제외되었습니다.";
        }

        log.info("[HistoryRetry] retry failed targets sendHistoryId={}, retryTargets={}, published={}, skipped={}",
                sendHistoryId, retryTargetCount, publishedCount, skippedCount);
        return retryResponse(sendHistoryId, retryTargetCount, publishedCount, skippedCount, message);
    }

    @Override
    public List<HistoryFilterOptionDTO> getChannelOptions() {
        return historyMapper.findChannelOptions();
    }

    @Override
    public List<HistoryFilterOptionDTO> getTagOptions() {
        return historyMapper.findTagOptions();
    }

    @Override
    public List<HistoryStatusOptionDTO> getStatusOptions() {
        return Arrays.stream(SendHistoryStatus.values())
                .map(status -> new HistoryStatusOptionDTO(status.getValue(), status.getLabel()))
                .toList();
    }

    @Override
    public List<String> getPurposeOptions() {
        return historyMapper.findPurposeOptions();
    }


    private String decryptKakaoTokenOrNull(SendHistoryVO campaign) {
        try {
            return tokenCryptoService.decrypt(campaign.getKakaoAccessTokenEnc());
        } catch (RuntimeException exception) {
            log.warn("[HistoryRetry] kakao token decrypt failed. retry continues without kakao token. sendHistoryId={}",
                    campaign.getId());
            return null;
        }
    }
    private MessageTaskDto buildRetryTask(SendHistoryVO campaign, String kakaoAccessToken,
                                          SendTargetVO target, RecipientSendPlan plan) {
        String unsubscribeUrl = shouldCreateUnsubscribeUrl(campaign, plan)
                ? shortUrlService.createTrackedUrl(target.getId(), null, ShortUrlPurpose.UNSUBSCRIBE)
                : null;

        return MessageTaskDto.builder()
                .messageId("retry-" + campaign.getId() + "-" + target.getId() + "-" + UUID.randomUUID())
                .sendHistoryId(campaign.getId())
                .sendTargetId(target.getId())
                .templateId(campaign.getTemplateId())
                .campaignId(campaign.getId())
                .customerId(plan.getCustomerId())
                .isRealCustomer(plan.getRecipient().getIsRealCustomer())
                .customerName(plan.getRecipient().getCustomerName())
                .userId(campaign.getUserId())
                .phoneNumber(plan.getRecipient().getPhone())
                .email(plan.getRecipient().getEmail())
                .kakaoUserKey(plan.getRecipient().getKakaoUserKey())
                .kakaoAccessToken(kakaoAccessToken)
                .title(campaign.getTitle())
                .content(campaign.getContent())
                .purpose(campaign.getPurpose())
                .actionButtonName(null)
                .actionUrl(null)
                .unsubscribeUrl(unsubscribeUrl)
                .fallbackSequence(plan.getFallbackSequence())
                .currentStep(0)
                .build();
    }

    private boolean shouldCreateUnsubscribeUrl(SendHistoryVO campaign, RecipientSendPlan plan) {
        if (campaign.getPurpose() == null || !"AD".equals(campaign.getPurpose().trim().toUpperCase(Locale.ROOT))) {
            return false;
        }
        return plan.getFallbackSequence() != null
                && plan.getFallbackSequence().stream()
                .map(this::normalizeChannelType)
                .anyMatch(channel -> "SMS".equals(channel) || "LMS".equals(channel));
    }

    private List<String> restorePriorities(List<Long> routingChannelIds, List<ChannelVO> activeChannels) {
        if (routingChannelIds == null) {
            return List.of();
        }
        return routingChannelIds.stream()
                .map(id -> activeChannels.stream().filter(channel -> channel.getId().equals(id)).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .map(ChannelVO::getChannelType)
                .toList();
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

    private HistoryRetryFailedResponseDTO retryResponse(Long sendHistoryId, int retryTargetCount,
                                                        int publishedCount, int skippedCount, String message) {
        return HistoryRetryFailedResponseDTO.builder()
                .sendHistoryId(sendHistoryId)
                .retryTargetCount(retryTargetCount)
                .publishedCount(publishedCount)
                .skippedCount(skippedCount)
                .message(message)
                .build();
    }

    private List<HistoryListResponseDTO> attachChannelsAndTags(List<HistoryListResponseDTO> histories) {
        if (histories.isEmpty()) {
            return histories;
        }

        List<Long> historyIds = histories.stream().map(HistoryListResponseDTO::id).toList();

        Map<Long, List<String>> channelsByHistoryId = historyMapper.findChannelsByHistoryIds(historyIds).stream()
                .collect(Collectors.groupingBy(HistoryChannelResponseDTO::sendHistoryId,
                        Collectors.mapping(HistoryChannelResponseDTO::channelName, Collectors.toList())));

        Map<Long, List<String>> tagsByHistoryId = historyMapper.findTagsByHistoryIds(historyIds).stream()
                .collect(Collectors.groupingBy(HistoryTagResponseDTO::sendHistoryId,
                        Collectors.mapping(HistoryTagResponseDTO::tagName, Collectors.toList())));

        return histories.stream()
                .map(history -> history
                .withChannels(channelsByHistoryId.getOrDefault(history.id(), Collections.emptyList()))
                .withTags(tagsByHistoryId.getOrDefault(history.id(), Collections.emptyList())))
                .toList();
    }
}
