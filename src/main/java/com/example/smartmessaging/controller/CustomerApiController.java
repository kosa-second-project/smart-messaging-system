package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.CustomerSearchRequest;
import com.example.smartmessaging.dto.response.PagedCustomerResponse;
import com.example.smartmessaging.service.CampaignDraftService;
import com.example.smartmessaging.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.smartmessaging.security.CustomUserDetails;

import java.util.List;

/**
 * 고객 데이터 REST API 컨트롤러
 * - PageController(뷰 반환)와 분리하여 JSON 응답만 담당
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerApiController {

    private final CustomerService customerService;
    private final CampaignDraftService draftService;

    /**
     * 고객 목록 조회
     * - draftId가 있을 때: Redis에서 현재 페이지 ID를 꺼낸 뒤 Oracle에서 상세 조회 (selected 탭 페이징)
     * - draftId가 없을 때: 기존 필터 조건 기반 Oracle 조회
     * GET /api/customers?keyword=김&tagIds=2&page=1&size=20
     * GET /api/customers?draftId=uuid&page=1&size=20
     */
    @GetMapping
    public ResponseEntity<PagedCustomerResponse> getCustomers(
            @ModelAttribute CustomerSearchRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String draftId = request.getDraftId();

        // activeTab이 selected이고 draftId가 유효한 경우: 수신자 관리 탭의 Redis 기반 페이징 처리
        if ("selected".equals(request.getActiveTab()) && draftId != null && !draftId.isBlank()) {
            // Redis에서 현재 페이지에 해당하는 ID만 꺼내기
            List<Long> pagedIds = draftService.getPagedIds(userDetails.getUserId(), draftId, request.getPage(), request.getSize());
            long totalCount     = draftService.getTotalCount(userDetails.getUserId(), draftId);

            if (pagedIds.isEmpty()) {
                return ResponseEntity.ok(PagedCustomerResponse.builder()
                        .content(List.of()).totalCount(0).page(request.getPage())
                        .size(request.getSize()).totalPages(0).build());
            }

            // 꺼낸 ID 목록으로 Oracle 상세 조회 (offset 0 고정)
            request.setCustomerIds(pagedIds);
            request.setPage(1);
            PagedCustomerResponse response = customerService.getCustomers(userDetails.getUserId(), request);

            // totalCount와 totalPages는 Redis 기준으로 재계산
            return ResponseEntity.ok(response.withTotalCount(totalCount));
        }

        // 일반 필터 탭인 경우 (request.getDraftId()가 담겨 있어 서비스 단에서 isInDraft 판별이 자동으로 됨)
        return ResponseEntity.ok(customerService.getCustomers(userDetails.getUserId(), request));
    }


    /**
     * 필터 조건에 맞는 고객 ID 전체 조회 (페이지네이션 없음)
     * GET /api/customers/ids?keyword=김&tagIds=2&tagIds=5
     */
    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getCustomerIds(
            @ModelAttribute CustomerSearchRequest request) {
        return ResponseEntity.ok(customerService.getCustomerIds(request));
    }
}
