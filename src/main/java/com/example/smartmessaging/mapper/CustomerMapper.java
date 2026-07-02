package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.request.CustomerSearchDTO;
import com.example.smartmessaging.dto.response.CustomerReceiveHistoryResponseDTO;
import com.example.smartmessaging.dto.response.CustomerResponseDTO;
import com.example.smartmessaging.dto.response.CustomerStatResponseDTO;
import com.example.smartmessaging.dto.response.RejectCustomerResponseDTO;
import com.example.smartmessaging.dto.request.CustomerSearchRequest;
import com.example.smartmessaging.dto.vo.CustomerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustomerMapper {

    List<CustomerResponseDTO> selectCustomerList(CustomerSearchDTO searchDTO);

    int selectCustomerCount(CustomerSearchDTO searchDTO);

    List<CustomerVO> findBySearch(CustomerSearchRequest request);

    CustomerStatResponseDTO selectCustomerStats();

    int countBySearch(CustomerSearchRequest request);

    List<String> selectCustomerTags(@Param("customerId") Long customerId);

    List<Long> findIdsBySearch(CustomerSearchRequest request);

    List<CustomerReceiveHistoryResponseDTO> selectCustomerReceiveHistory(@Param("customerId") Long customerId);

    List<RejectCustomerResponseDTO> selectRejectCustomerList(CustomerSearchDTO searchDTO);

    int selectRejectCustomerCount(CustomerSearchDTO searchDTO);

    List<Map<String, Object>> findTagsByCustomerIds(@Param("customerIds") List<Long> customerIds);
}
