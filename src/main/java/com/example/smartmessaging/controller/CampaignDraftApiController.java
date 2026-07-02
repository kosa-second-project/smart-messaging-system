package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.CustomerSearchRequest;
import com.example.smartmessaging.service.CampaignDraftService;
import com.example.smartmessaging.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 발송 대상자 Draft(임시 저장) API 컨트롤러
 *
 * POST   /api/campaigns/draft                        - 조건 기반 전체 ID → Redis 저장 → draftId 반환
 * PATCH  /api/campaigns/draft/{draftId}/recipients   - 개별 추가/제거 배치 동기화
 * DELETE /api/campaigns/draft/{draftId}              - 발송 완료/취소 시 정리
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
    public ResponseEntity<Map<String, Object>> createEmptyDraft() {
        String draftId = draftService.createEmptyDraft();
        return ResponseEntity.ok(Map.of("draftId", draftId, "totalCount", 0));
    }

    /**
     * 1단계 [다음] 클릭 시: 필터 조건으로 전체 ID 조회 → Redis 저장 → draftId 반환
     * POST /api/campaigns/draft
     */
    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> createOrAppendDraft(
            @ModelAttribute CustomerSearchRequest request,
            @RequestParam(required = false) String draftId) {

        // 기존 CustomerService의 getCustomerIds를 재활용하여 전체 ID 목록 조회
        List<Long> allIds = customerService.getCustomerIds(request);

        if (allIds == null) {
            allIds = List.of();
        }

        String finalDraftId;
        // draftId 파라미터가 존재하면 기존 항목에 병합(Append), 없으면 새로 생성(Save)
        if (draftId != null && !draftId.isBlank()) {
            finalDraftId = draftService.appendDraft(draftId, allIds);
        } else {
            finalDraftId = draftService.saveDraft(allIds);
        }

        long totalCount = draftService.getTotalCount(finalDraftId);
        log.info("[Draft 일괄 처리] draftId={}, 최종 {}명", finalDraftId, totalCount);
        return ResponseEntity.ok(Map.of("draftId", finalDraftId, "totalCount", totalCount));
    }

    /**
     * 개별 체크박스 추가/제거 배치 동기화 (낙관적 업데이트와 함께 사용)
     * PATCH /api/campaigns/draft/{draftId}/recipients
     * Body: { "items": [{"customerId": 5, "action": "REMOVE"}, {"customerId": 12, "action": "ADD"}] }
     */
    `@PatchMapping`("/draft/{draftId}/recipients")
    public ResponseEntity<Map<String, Object>> updateRecipients(
            `@PathVariable` String draftId,
            `@RequestBody` Map<String, List<Map<String, Object>>> body) {

        List<Map<String, Object>> items = body.get("items");
        if (items == null || items.isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }

        for (Map<String, Object> item : items) {
            Object rawId = item.get("customerId");
            Object rawAction = item.get("action");
            if (rawId == null || rawAction == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "customerId and action are required"));
            }
            try {
                Long customerId = Long.parseLong(rawId.toString());
                String action = rawAction.toString();
                if ("REMOVE".equals(action)) {
                    draftService.removeRecipient(draftId, customerId);
                } else if ("ADD".equals(action)) {
                    draftService.addRecipient(draftId, customerId);
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "invalid customerId: " + rawId));
            }
        }

        long totalCount = draftService.getTotalCount(draftId);
        return ResponseEntity.ok(Map.of("success", true, "totalCount", totalCount));
    }

    /**
     * 발송 완료 또는 취소 시 Draft 명시적 정리
     * DELETE /api/campaigns/draft/{draftId}
     */
    @DeleteMapping("/draft/{draftId}")
    public ResponseEntity<Map<String, Object>> deleteDraft(@PathVariable String draftId) {
        draftService.deleteDraft(draftId);
        return ResponseEntity.ok(Map.of("success", true));
    }

}
