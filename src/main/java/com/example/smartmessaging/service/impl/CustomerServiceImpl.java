package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.CustomerSearchDTO;
import com.example.smartmessaging.dto.response.CustomerReceiveHistoryResponseDTO;
import com.example.smartmessaging.dto.response.CustomerResponseDTO;
import com.example.smartmessaging.dto.response.CustomerStatResponseDTO;
import com.example.smartmessaging.dto.response.PageResponse;
import com.example.smartmessaging.dto.response.RejectCustomerResponseDTO;
import com.example.smartmessaging.mapper.CustomerMapper;
import com.example.smartmessaging.service.CustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

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

}
