package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.response.CostEstimationResponseDTO;
import java.util.List;
import java.util.Map;

/**
 * 발송 대상자 임시 보관(Draft) 서비스 인터페이스
 */
public interface CampaignDraftService {

    String saveDraft(Long userId, List<Long> customerIds);

    String appendDraft(Long userId, String draftId, List<Long> customerIds);

    String createEmptyDraft(Long userId);

    List<Long> getPagedIds(Long userId, String draftId, int page, int size);

    List<Long> getDraftCustomerIds(Long userId, String draftId);

    long getTotalCount(Long userId, String draftId);

    void markProcessing(Long userId, String draftId, long expectedCount);

    void markReady(Long userId, String draftId, long totalCount);

    void markFailed(Long userId, String draftId);

    Map<String, Object> getStatus(Long userId, String draftId);

    void removeRecipient(Long userId, String draftId, Long customerId);

    void addRecipient(Long userId, String draftId, Long customerId);

    void removeRecipients(Long userId, String draftId, List<Long> customerIds);

    void addRecipients(Long userId, String draftId, List<Long> customerIds);

    boolean isExpired(Long userId, String draftId);

    void deleteDraft(Long userId, String draftId);

    Map<Long, Boolean> getRecipientStatusMap(Long userId, String draftId, List<Long> customerIds);

    List<Long> getAllIds(Long userId, String draftId);

    CostEstimationResponseDTO estimateCost(Long userId, String draftId, List<String> priorities);
}
