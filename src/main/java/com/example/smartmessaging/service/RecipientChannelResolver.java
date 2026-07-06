package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.model.SendRecipientCandidateVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RecipientChannelResolver {

    public List<RecipientSendPlan> resolve(
            List<Long> customerIds,
            List<SendRecipientCandidateVO> candidates,
            List<Long> priorityChannelIds,
            String content) {
        Map<Long, Map<Long, SendRecipientCandidateVO>> byCustomer = candidates.stream()
                .collect(Collectors.groupingBy(
                        SendRecipientCandidateVO::getCustomerId,
                        LinkedHashMap::new,
                        Collectors.toMap(
                                SendRecipientCandidateVO::getChannelId,
                                candidate -> candidate,
                                (left, right) -> left,
                                LinkedHashMap::new)));

        Map<Long, SendRecipientCandidateVO> channelsById = candidates.stream()
                .filter(candidate -> candidate.getChannelId() != null)
                .collect(Collectors.toMap(
                        SendRecipientCandidateVO::getChannelId,
                        candidate -> candidate,
                        (left, right) -> left,
                        HashMap::new));

        List<Long> orderedCustomers = customerIds == null ? List.of() : customerIds;
        List<Long> priorities = priorityChannelIds == null ? List.of() : priorityChannelIds;

        return orderedCustomers.stream()
                .map(customerId -> resolveOne(customerId, byCustomer.get(customerId), channelsById, priorities, content))
                .toList();
    }

    private RecipientSendPlan resolveOne(
            Long customerId,
            Map<Long, SendRecipientCandidateVO> candidateByChannel,
            Map<Long, SendRecipientCandidateVO> channelsById,
            List<Long> priorityChannelIds,
            String content) {
        if (candidateByChannel == null || candidateByChannel.isEmpty()) {
            return skipped(customerId, "NO_ACTIVE_CHANNEL");
        }

        List<SendRecipientCandidateVO> sequence = new ArrayList<>();
        for (Long requestedChannelId : priorityChannelIds) {
            SendRecipientCandidateVO requested = candidateByChannel.get(requestedChannelId);
            SendRecipientCandidateVO candidate = chooseContentAwareCandidate(requested, candidateByChannel, channelsById, content)
                    .orElse(null);
            if (candidate != null && isUsable(candidate) && sequence.stream().noneMatch(item -> Objects.equals(item.getChannelId(), candidate.getChannelId()))) {
                sequence.add(candidate);
            }
        }

        if (sequence.isEmpty()) {
            return skipped(customerId, "NO_CONSENTED_CONTACT");
        }

        SendRecipientCandidateVO first = sequence.get(0);
        return RecipientSendPlan.builder()
                .customerId(customerId)
                .channelSequence(sequence.stream().map(SendRecipientCandidateVO::getChannelId).toList())
                .firstChannelId(first.getChannelId())
                .firstChannelType(first.getChannelType())
                .estimatedCost(first.getCostPerMsg() == null ? BigDecimal.ZERO : first.getCostPerMsg())
                .sendable(true)
                .build();
    }

    private Optional<SendRecipientCandidateVO> chooseContentAwareCandidate(
            SendRecipientCandidateVO requested,
            Map<Long, SendRecipientCandidateVO> candidateByChannel,
            Map<Long, SendRecipientCandidateVO> channelsById,
            String content) {
        if (requested == null) {
            return Optional.empty();
        }
        if (!isSms(requested) || requested.getMaxLength() == null || messageByteLength(content) <= requested.getMaxLength()) {
            return Optional.of(requested);
        }

        return candidateByChannel.values().stream()
                .filter(this::isLms)
                .min(Comparator.comparing(SendRecipientCandidateVO::getChannelId))
                .or(() -> channelsById.values().stream().filter(this::isLms).findFirst())
                .map(lms -> candidateByChannel.getOrDefault(lms.getChannelId(), requested));
    }

    private RecipientSendPlan skipped(Long customerId, String reason) {
        return RecipientSendPlan.builder()
                .customerId(customerId)
                .channelSequence(List.of())
                .estimatedCost(BigDecimal.ZERO)
                .sendable(false)
                .skipReason(reason)
                .build();
    }

    private boolean isUsable(SendRecipientCandidateVO candidate) {
        return Boolean.TRUE.equals(candidate.getConsented()) && hasRecipientValue(candidate);
    }

    public String recipientValue(SendRecipientCandidateVO candidate) {
        if (candidate == null) {
            return null;
        }
        String type = normalize(candidate.getChannelType());
        if ("EMAIL".equals(type)) {
            return blankToNull(candidate.getEmail());
        }
        return blankToNull(candidate.getPhone());
    }

    private boolean hasRecipientValue(SendRecipientCandidateVO candidate) {
        return recipientValue(candidate) != null;
    }

    private boolean isSms(SendRecipientCandidateVO candidate) {
        return "SMS".equals(normalize(candidate.getChannelType()));
    }

    private boolean isLms(SendRecipientCandidateVO candidate) {
        return "LMS".equals(normalize(candidate.getChannelType()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int messageByteLength(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        int byteLength = 0;
        for (int i = 0; i < content.length(); i++) {
            byteLength += content.charAt(i) <= 0x007F ? 1 : 2;
        }
        return byteLength;
    }
}
