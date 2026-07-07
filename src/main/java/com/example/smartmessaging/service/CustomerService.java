package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.CustomerSearchDTO;
import com.example.smartmessaging.dto.request.CustomerSearchRequest;
import com.example.smartmessaging.dto.response.CustomerReceiveHistoryResponseDTO;
import com.example.smartmessaging.dto.response.CustomerResponseDTO;
import com.example.smartmessaging.dto.response.CustomerStatResponseDTO;
import com.example.smartmessaging.dto.response.PageResponse;
import com.example.smartmessaging.dto.response.PagedCustomerResponse;
import com.example.smartmessaging.dto.response.RejectCustomerResponseDTO;
import com.example.smartmessaging.dto.vo.TagVO;

import java.util.List;

public interface CustomerService {
    PageResponse<CustomerResponseDTO> getCustomerList(CustomerSearchDTO searchDTO);

    List<TagVO> getTags();

    CustomerResponseDTO getCustomerById(Long customerId);

    CustomerStatResponseDTO getCustomerStats();

    List<CustomerReceiveHistoryResponseDTO> getCustomerReceiveHistory(Long customerId);

    PageResponse<RejectCustomerResponseDTO> getRejectCustomerList(CustomerSearchDTO searchDTO);

    PagedCustomerResponse getCustomers(Long userId, CustomerSearchRequest request);

    List<Long> getCustomerIds(CustomerSearchRequest request);
}