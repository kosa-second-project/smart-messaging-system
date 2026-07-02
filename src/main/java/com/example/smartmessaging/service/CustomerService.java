package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.CustomerSearchDTO;
import com.example.smartmessaging.dto.response.CustomerReceiveHistoryResponseDTO;
import com.example.smartmessaging.dto.response.CustomerResponseDTO;
import com.example.smartmessaging.dto.response.CustomerStatResponseDTO;
import com.example.smartmessaging.dto.response.PageResponse;
import com.example.smartmessaging.dto.response.RejectCustomerResponseDTO;
import java.util.List;

public interface CustomerService {

    /**
     * 필터 및 검색 조건을 적용하여 고객 목록을 조회합니다. (페이징 지원)
     */
    PageResponse<CustomerResponseDTO> getCustomerList(CustomerSearchDTO searchDTO);

    /**
     * 고객 유형별 집계 통계 수치를 조회합니다.
     */
    CustomerStatResponseDTO getCustomerStats();



    /**
     * 특정 고객의 과거 메시지 수신 내역을 조회합니다.
     */
    List<CustomerReceiveHistoryResponseDTO> getCustomerReceiveHistory(Long customerId);

    /**
     * 080 수신거부된 고객 목록을 조회합니다. (페이징 지원)
     */
    PageResponse<RejectCustomerResponseDTO> getRejectCustomerList(CustomerSearchDTO searchDTO);
}

