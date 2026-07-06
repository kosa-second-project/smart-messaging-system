package com.example.smartmessaging.service.repository;

import com.example.smartmessaging.dto.vo.SendRealRecipientWhitelistVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SendRealRecipientWhitelistMapper {
    boolean existsActiveRecipient(@Param("phone") String phone,
                                  @Param("email") String email,
                                  @Param("channelId") Long channelId);

    int insertWhitelistRecipient(SendRealRecipientWhitelistVO whitelist);
}
