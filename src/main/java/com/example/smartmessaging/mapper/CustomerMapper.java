package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.request.CustomerSearchDTO;
import com.example.smartmessaging.dto.response.CustomerReceiveHistoryResponseDTO;
import com.example.smartmessaging.dto.response.CustomerResponseDTO;
import com.example.smartmessaging.dto.response.CustomerStatResponseDTO;
import com.example.smartmessaging.dto.response.PageResponse;
import com.example.smartmessaging.dto.response.RejectCustomerResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CustomerMapper {

    /**
     * 필터 조건 및 페이징을 적용하여 고객 목록을 조회합니다.
     */
    List<CustomerResponseDTO> selectCustomerList(CustomerSearchDTO searchDTO);

    /**
     * 필터 조건에 부합하는 총 고객 수를 조회합니다. (페이징용)
     */
    int selectCustomerCount(CustomerSearchDTO searchDTO);

    /**
     * 고객 유형별 집계 통계 데이터를 조회합니다.
     */
    CustomerStatResponseDTO selectCustomerStats();


    /**
     * 특정 고객의 타겟 태그 목록을 조회합니다. (MyBatis 컬렉션 매핑용)
     */
    List<String> selectCustomerTags(@Param("customerId") Long customerId);

    /**
     * 특정 고객의 과거 메시지 수신 이력을 조회합니다.
     */
    List<CustomerReceiveHistoryResponseDTO> selectCustomerReceiveHistory(@Param("customerId") Long customerId);

    /**
     * 080 수신 거부 등록된 고객 목록을 조회합니다.
     */
    List<RejectCustomerResponseDTO> selectRejectCustomerList(CustomerSearchDTO searchDTO);

    /**
     * 080 수신 거부 등록된 총 고객 수를 조회합니다. (페이징용)
     */
    int selectRejectCustomerCount(CustomerSearchDTO searchDTO);
}

