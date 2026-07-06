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

    CustomerStatResponseDTO selectCustomerStats();

    List<String> selectCustomerTags(@Param("customerId") Long customerId);

    List<CustomerReceiveHistoryResponseDTO> selectCustomerReceiveHistory(@Param("customerId") Long customerId);

    List<RejectCustomerResponseDTO> selectRejectCustomerList(CustomerSearchDTO searchDTO);

    int selectRejectCustomerCount(CustomerSearchDTO searchDTO);

    /**
     * 검색어 + 태그 필터 + 페이징으로 고객 목록을 조회합니다.
     */
    List<CustomerVO> findBySearch(CustomerSearchRequest request);

    /**
     * 검색어 + 태그 필터 조건의 전체 고객 수를 조회합니다. (페이징 계산용)
     */
    int countBySearch(CustomerSearchRequest request);

    /**
     * 검색어 + 태그 필터 조건에 맞는 고객 ID 전체를 조회합니다. (페이지네이션 없음)
     * "필터 결과 전체 선택" 기능에서 사용
     */
    List<Long> findIdsBySearch(CustomerSearchRequest request);

    /**
     * 고객 ID 목록으로 태그 이름을 일괄 조회합니다. (N+1 방지 SELECT IN)
     * 반환값: [{CUSTOMER_ID: 1, TAG_NAME: "남자"}, ...]
     */
    List<Map<String, Object>> findTagsByCustomerIds(@Param("customerIds") List<Long> customerIds);

    CustomerVO findByPhone(@Param("phone") String phone);

    int insertTestCustomer(CustomerVO customer);

    int updateTestCustomer(CustomerVO customer);

    int upsertChannelConsent(
            @Param("customerId") Long customerId,
            @Param("channelType") String channelType,
            @Param("actorId") Long actorId
    );

    int upsertTestCustomerTag(
            @Param("customerId") Long customerId,
            @Param("tagId") Long tagId,
            @Param("actorId") Long actorId
    );
}
