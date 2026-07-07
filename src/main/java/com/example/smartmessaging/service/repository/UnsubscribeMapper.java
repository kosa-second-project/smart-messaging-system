package com.example.smartmessaging.service.repository;

import com.example.smartmessaging.dto.vo.ShortUrlTargetVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UnsubscribeMapper {
    ShortUrlTargetVO findTargetBySendTargetId(@Param("sendTargetId") Long sendTargetId);

    int countRejectHistory(@Param("customerId") Long customerId);

    int insertRejectHistory(@Param("customerId") Long customerId, @Param("createdBy") Long createdBy);

    int insertRejectHistoryIfAbsent(@Param("customerId") Long customerId, @Param("createdBy") Long createdBy);

    int blockCustomerAds(@Param("customerId") Long customerId, @Param("updatedBy") Long updatedBy);

    int revokeSmsConsent(@Param("customerId") Long customerId, @Param("updatedBy") Long updatedBy);
}
