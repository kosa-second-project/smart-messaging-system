package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.CustomerSearchDTO;
import com.example.smartmessaging.dto.request.CustomerSearchRequest;
import com.example.smartmessaging.dto.response.*;
import com.example.smartmessaging.dto.vo.CustomerVO;
import com.example.smartmessaging.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.smartmessaging.dto.request.CustomerSearchRequest;
import com.example.smartmessaging.dto.response.CustomerSummaryResponse;
import com.example.smartmessaging.dto.response.PagedCustomerResponse;
import com.example.smartmessaging.dto.vo.CustomerVO;
import com.example.smartmessaging.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final CampaignDraftService draftService;

    // ==========================================
    // 1. 고객 관리 탭 비즈니스 로직 (Customer Management)
    // ==========================================

    @Override
    public PageResponse<CustomerResponseDTO> getCustomerList(CustomerSearchDTO searchDTO) {
        log.debug("[CustomerService] 고객 목록 조회 요청 - 필터: {}", searchDTO);
        List<CustomerResponseDTO> list = customerMapper.selectCustomerList(searchDTO);
        int totalCount = customerMapper.selectCustomerCount(searchDTO);
        return new PageResponse<>(list, totalCount, searchDTO.getPage(), searchDTO.getSize());
    }

    @Override
    public CustomerStatResponseDTO getCustomerStats() {
        log.debug("[CustomerService] 고객 유형별 집계 통계 조회 요청");
        return customerMapper.selectCustomerStats();
    }

    @Override
    public List<CustomerReceiveHistoryResponseDTO> getCustomerReceiveHistory(Long customerId) {
        log.debug("[CustomerService] 고객 수신 이력 조회 요청 - CustomerID: {}", customerId);
        return customerMapper.selectCustomerReceiveHistory(customerId);
    }

    @Override
    public PageResponse<RejectCustomerResponseDTO> getRejectCustomerList(CustomerSearchDTO searchDTO) {
        log.debug("[CustomerService] 080 수신거부자 목록 조회 요청 - 필터: {}", searchDTO);
        List<RejectCustomerResponseDTO> list = customerMapper.selectRejectCustomerList(searchDTO);
        int totalCount = customerMapper.selectRejectCustomerCount(searchDTO);
        return new PageResponse<>(list, totalCount, searchDTO.getPage(), searchDTO.getSize());
    }

    // ==========================================
    // 2. 메시지 발송 탭 비즈니스 로직 (Campaign Sending Recipient Selection)
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public PagedCustomerResponse getCustomers(Long userId, CustomerSearchRequest request) {
        log.debug("[CustomerService] 메시지 발송용 고객 목록 조회 요청 - ActiveTab: {}", request.getActiveTab());

        // 1. 고객 목록 조회
        List<CustomerVO> customers = customerMapper.findBySearch(request);

        // 2. 총 건수 조회 (페이징 계산용)
        int totalCount = customerMapper.countBySearch(request);

        // 3. 조회된 고객이 없으면 빈 응답 반환
        if (customers.isEmpty()) {
            return PagedCustomerResponse.builder()
                    .content(Collections.emptyList())
                    .totalCount(0)
                    .page(request.getPage())
                    .size(request.getSize())
                    .totalPages(0)
                    .build();
        }

        // 4. 고객 ID 목록 추출 후 태그 일괄 조회 (N+1 방지)
        List<Long> customerIds = customers.stream()
                .map(CustomerVO::getId)
                .collect(Collectors.toList());

        List<Map<String, Object>> tagRows = customerMapper.findTagsByCustomerIds(customerIds);

        // 5. 고객 ID 기준으로 태그 이름 목록 그룹핑
        Map<Long, List<String>> tagMap = tagRows.stream()
                .collect(Collectors.groupingBy(
                        row -> ((Number) row.get("CUSTOMER_ID")).longValue(),
                        Collectors.mapping(row -> (String) row.get("TAG_NAME"), Collectors.toList())
                ));

        // 5.1. Redis Draft에 포함된 수신자 임시저장 상태 조회 (O(1) 체크용 맵 생성)
        Map<Long, Boolean> draftRecipientStatusMap;
        if ("selected".equals(request.getActiveTab())) {
            // selected 탭은 무조건 모두 임시 저장되어 있는 회원이므로 Redis 조회 생략 (전원 true 강제)
            draftRecipientStatusMap = Map.of();
        } else {
            draftRecipientStatusMap = draftService.getRecipientStatusMap(userId, request.getDraftId(), customerIds);
        }

        // 6. VO → Response DTO 변환
        List<CustomerSummaryResponse> content = customers.stream()
                .map(vo -> {
                    boolean isInDraft = "selected".equals(request.getActiveTab()) 
                            || draftRecipientStatusMap.getOrDefault(vo.getId(), false);
                    return CustomerSummaryResponse.builder()
                            .id(vo.getId())
                            .name(vo.getName())
                            .phone(maskPhoneNumber(vo.getPhone()))
                            .tags(tagMap.getOrDefault(vo.getId(), Collections.emptyList()))
                            .isInDraft(isInDraft)
                            .build();
                })
                .collect(Collectors.toList());

        // 7. 페이징 메타 계산 후 반환
        // 다음 커서 ID 계산 (조회된 목록의 마지막 ID)
        Long nextCursorId = content.isEmpty() ? null : content.get(content.size() - 1).getId();
        boolean hasNext = content.size() == request.getSize();

        // 7. 페이징 메타 계산 후 반환 (totalCount/totalPages는 UI 편의상 유지 가능)
        int totalPages = request.getSize() > 0 ? (int) Math.ceil((double) totalCount / request.getSize()) : 0;

        return PagedCustomerResponse.builder()
                .content(content)
                .totalCount(totalCount)
                .page(request.getPage())
                .size(request.getSize())
                .totalPages(totalPages)
                .nextCursorId(nextCursorId)
                .hasNext(hasNext)
                .build();
    }

    // ==========================================
    // 3. 내부 유틸리티 메서드
    // ==========================================

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return "-";
        }
        // 숫자만 남깁니다.
        String clean = phone.replaceAll("[^0-9]", "");
        if (clean.length() == 11) {
            return clean.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1-****-$2");
        } else if (clean.length() == 10) {
            return clean.replaceAll("(\\d{3})\\d{3}(\\d{4})", "$1-***-$2");
        }
        return phone; // 기타 규격 외 번호는 그대로 반환
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getCustomerIds(CustomerSearchRequest request) {
        // 페이지네이션 없이 필터 조건에 맞는 전체 고객 ID 목록 반환
        // "필터 결과 전체 선택" 기능에서 사용
        return customerMapper.findIdsBySearch(request);
    }
}
