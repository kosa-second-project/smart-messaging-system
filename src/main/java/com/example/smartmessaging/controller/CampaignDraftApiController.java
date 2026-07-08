package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.CustomerSearchRequest;
import com.example.smartmessaging.service.CampaignDraftService;
import com.example.smartmessaging.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.smartmessaging.security.CustomUserDetails;
import com.example.smartmessaging.dto.request.UpdateRecipientsRequest;
import com.example.smartmessaging.dto.request.RecipientItem;
import com.example.smartmessaging.dto.response.CostEstimationResponseDTO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 발송 대상자 Draft(임시 저장) API 컨트롤러
 *
 * POST /api/campaigns/draft - 조건 기반 전체 ID → Redis 저장 → draftId 반환
 * PATCH /api/campaigns/draft/{draftId}/recipients - 개별 추가/제거 배치 동기화
 * DELETE /api/campaigns/draft/{draftId} - 발송 완료/취소 시 정리
 */
@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignDraftApiController {

    private final CampaignDraftService draftService;
    private final CustomerService customerService;

    /**
     * 1.5. 아무 조건 없는 빈 Draft 생성
     * POST /api/campaigns/draft/empty
     */
    @PostMapping("/draft/empty")
    public ResponseEntity<Map<String, Object>> createEmptyDraft(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String draftId = draftService.createEmptyDraft(userDetails.getUserId());
        return ResponseEntity.ok(Map.of("draftId", draftId, "totalCount", 0));
    }

    /**
     * 1단계 [다음] 클릭 시: 필터 조건으로 전체 ID 조회 → Redis 저장 → draftId 반환
     * POST /api/campaigns/draft
     */
    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> createOrAppendDraft(
            @ModelAttribute CustomerSearchRequest request,
            @RequestParam(required = false) String draftId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        long startedAt = System.currentTimeMillis();
        Long userId = userDetails.getUserId();
        String finalDraftId = (draftId != null && !draftId.isBlank())
                ? draftId
                : draftService.createEmptyDraft(userId);
        CustomerSearchRequest requestSnapshot = copyRequest(request);

        long existingCount = (draftId != null && !draftId.isBlank()) ? draftService.getTotalCount(userId, finalDraftId) : 0L;
        draftService.markProcessing(userId, finalDraftId, existingCount);

        CompletableFuture.runAsync(() -> populateDraftAsync(userId, finalDraftId, requestSnapshot));

        long completedAt = System.currentTimeMillis();
        log.info("[Draft 일괄 처리 접수] draftId={}, 기존 {}명, accept={}ms", finalDraftId, existingCount, completedAt - startedAt);
        return ResponseEntity.ok(Map.of(
                "draftId", finalDraftId,
                "totalCount", existingCount,
                "status", "PROCESSING"
        ));
    }

    private void populateDraftAsync(Long userId, String draftId, CustomerSearchRequest request) {
        long startedAt = System.currentTimeMillis();
        try {
            List<Long> allIds = customerService.getCustomerIds(request);
            long idsLoadedAt = System.currentTimeMillis();
            if (allIds == null) {
                allIds = List.of();
            }

            draftService.appendDraft(userId, draftId, allIds);
            long draftSavedAt = System.currentTimeMillis();

            long totalCount = draftService.getTotalCount(userId, draftId);
            draftService.markReady(userId, draftId, totalCount);
            long completedAt = System.currentTimeMillis();
            log.info("[Draft 비동기 적재 완료] draftId={}, 최종 {}명, idsLoad={}ms, redisSave={}ms, totalCount={}ms, total={}ms",
                    draftId,
                    totalCount,
                    idsLoadedAt - startedAt,
                    draftSavedAt - idsLoadedAt,
                    completedAt - draftSavedAt,
                    completedAt - startedAt);
        } catch (Exception e) {
            draftService.markFailed(userId, draftId);
            log.error("[Draft 비동기 적재 실패] draftId={}", draftId, e);
        }
    }

    /**
     * 개별 체크박스 추가/제거 배치 동기화 (낙관적 업데이트와 함께 사용)
     * PATCH /api/campaigns/draft/{draftId}/recipients
     * Body: { "items": [{"customerId": 5, "action": "REMOVE"}, {"customerId": 12,
     * "action": "ADD"}] }
     */
    @PatchMapping("/draft/{draftId}/recipients")
    public ResponseEntity<Map<String, Object>> updateRecipients(
            @PathVariable String draftId,
            @Valid @RequestBody UpdateRecipientsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }

        List<Long> addIds = new java.util.ArrayList<>();
        List<Long> removeIds = new java.util.ArrayList<>();
        for (RecipientItem item : request.getItems()) {
            Long customerId = item.getCustomerId();
            String action = item.getAction();
            
            if ("REMOVE".equals(action)) {
                removeIds.add(customerId);
            } else if ("ADD".equals(action)) {
                addIds.add(customerId);
            }
        }

        draftService.removeRecipients(userDetails.getUserId(), draftId, removeIds);
        draftService.addRecipients(userDetails.getUserId(), draftId, addIds);

        long totalCount = draftService.getTotalCount(userDetails.getUserId(), draftId);
        return ResponseEntity.ok(Map.of("success", true, "totalCount", totalCount));
    }

    /**
     * 발송 완료 또는 취소 시 Draft 명시적 정리
     * DELETE /api/campaigns/draft/{draftId}
     */
    @DeleteMapping("/draft/{draftId}")
    public ResponseEntity<Map<String, Object>> deleteDraft(
            @PathVariable String draftId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        draftService.deleteDraft(userDetails.getUserId(), draftId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * sendBeacon 호환용 POST 삭제 엔드포인트
     */
    @PostMapping("/draft/{draftId}/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupDraft(
            @PathVariable String draftId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        draftService.deleteDraft(userDetails.getUserId(), draftId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/draft/{draftId}/status")
    public ResponseEntity<Map<String, Object>> getDraftStatus(
            @PathVariable String draftId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(draftService.getStatus(userDetails.getUserId(), draftId));
    }

    @GetMapping("/draft/candidate-count")
    public ResponseEntity<Map<String, Object>> getCandidateCount(
            @ModelAttribute CustomerSearchRequest request) {
        int count = customerService.countCustomers(request);
        return ResponseEntity.ok(Map.of("totalCount", count));
    }

    /**
     * Draft 예상 비용 산출 및 채널 자동 배정 결과 조회
     * GET /api/campaigns/draft/{draftId}/estimate-cost
     */
    @GetMapping("/draft/{draftId}/estimate-cost")
    public ResponseEntity<CostEstimationResponseDTO> estimateCost(
            @PathVariable String draftId,
            @RequestParam(required = false) List<String> priorities,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(draftService.estimateCost(userDetails.getUserId(), draftId, priorities));
    }

    private CustomerSearchRequest copyRequest(CustomerSearchRequest source) {
        CustomerSearchRequest copy = new CustomerSearchRequest();
        copy.setKeyword(source.getKeyword());
        copy.setTagIds(source.getTagIds() == null ? null : new java.util.ArrayList<>(source.getTagIds()));
        copy.setCustomerIds(source.getCustomerIds() == null ? null : new java.util.ArrayList<>(source.getCustomerIds()));
        copy.setDraftId(source.getDraftId());
        copy.setActiveTab(source.getActiveTab());
        copy.setConsentTagIds(source.getConsentTagIds() == null ? List.of() : new java.util.ArrayList<>(source.getConsentTagIds()));
        copy.setMatchType(source.getMatchType());
        copy.setCursorId(source.getCursorId());
        copy.setPage(source.getPage());
        copy.setSize(source.getSize());
        return copy;
    }

}
