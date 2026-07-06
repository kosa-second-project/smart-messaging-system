package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.dto.queue.MessageQueueDto;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.service.repository.SendQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueFailureHandler {

    private final SendQueueRepository sendQueueRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCampaignFailed(Long sendHistoryId, Long userId, String reason) {
        if (sendHistoryId == null) {
            return;
        }
        sendQueueRepository.updateSendHistoryStatus(sendHistoryId, "FAILED", userId);
        log.warn("Campaign marked FAILED. historyId={}, reason={}", sendHistoryId, reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markMessageFailed(MessageQueueDto message, String reason) {
        if (message == null || message.getSendTargetId() == null) {
            return;
        }
        markTargetFailedInternal(
                message.getSendTargetId(),
                message.getSendHistoryId(),
                message.getUserId(),
                reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markTargetFailed(Long sendTargetId, Long sendHistoryId, Long userId, String reason) {
        if (sendTargetId == null) {
            return;
        }
        markTargetFailedInternal(sendTargetId, sendHistoryId, userId, reason);
    }

    private void markTargetFailedInternal(Long sendTargetId, Long sendHistoryId, Long userId, String reason) {
        SendTargetVO target = sendQueueRepository.findSendTargetById(sendTargetId);
        if (target == null) {
            log.warn("Unable to mark missing target FAILED. targetId={}, reason={}", sendTargetId, reason);
            return;
        }

        Long resolvedHistoryId = sendHistoryId != null ? sendHistoryId : target.getSendHistoryId();
        Long resolvedUserId = resolveUserId(resolvedHistoryId, userId);

        sendQueueRepository.updateSendTargetStatus(
                target.getId(),
                "FAILED",
                target.getFinalChannelId(),
                BigDecimal.ZERO,
                resolvedUserId);

        if (resolvedHistoryId != null) {
            sendQueueRepository.refreshHistoryCounters(resolvedHistoryId, resolvedUserId);
            if (sendQueueRepository.countUnfinishedTargets(resolvedHistoryId) == 0) {
                sendQueueRepository.markHistoryTerminal(resolvedHistoryId, resolvedUserId);
            }
        }
        log.warn("Target marked FAILED. targetId={}, historyId={}, reason={}", sendTargetId, resolvedHistoryId, reason);
    }

    private Long resolveUserId(Long sendHistoryId, Long fallbackUserId) {
        if (fallbackUserId != null || sendHistoryId == null) {
            return fallbackUserId;
        }
        SendHistoryVO history = sendQueueRepository.findSendHistoryById(sendHistoryId);
        return history == null ? null : history.getUserId();
    }
}
