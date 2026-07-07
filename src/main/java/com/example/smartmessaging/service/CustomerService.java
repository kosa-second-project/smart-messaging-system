package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.CustomerSearchDTO;
import com.example.smartmessaging.dto.response.CustomerReceiveHistoryResponseDTO;
import com.example.smartmessaging.dto.response.CustomerResponseDTO;
import com.example.smartmessaging.dto.response.CustomerStatResponseDTO;
import com.example.smartmessaging.dto.response.PageResponse;
import com.example.smartmessaging.dto.response.RejectCustomerResponseDTO;
import com.example.smartmessaging.dto.request.CustomerSearchRequest;
import com.example.smartmessaging.dto.response.PagedCustomerResponse;

import java.util.List;

public interface CustomerService {
    PageResponse<CustomerResponseDTO> getCustomerList(CustomerSearchDTO searchDTO);

    CustomerResponseDTO getCustomerById(Long customerId);

    CustomerStatResponseDTO getCustomerStats();

    List<CustomerReceiveHistoryResponseDTO> getCustomerReceiveHistory(Long customerId);

    PageResponse<RejectCustomerResponseDTO> getRejectCustomerList(CustomerSearchDTO searchDTO);

    /**
     * 고객 목록을 조회합니다.
     * - keyword: 고객명/전화번호 검색
     * - tagIds: 태그 기반 필터 (성별, 나이, 유형, 수신동의 등)
     * - page, size: 페이징
     */
    PagedCustomerResponse getCustomers(Long userId, CustomerSearchRequest request);

    /**
     * 필터 조건에 맞는 고객 ID 전체를 조회합니다. (페이지네이션 없음)
     * "필터 결과 전체 선택" 기능에서 사용
     */
    List<Long> getCustomerIds(CustomerSearchRequest request);
}
