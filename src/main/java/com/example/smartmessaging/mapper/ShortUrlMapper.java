package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.vo.ShortUrlVO;
import com.example.smartmessaging.dto.vo.ShortUrlTargetVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShortUrlMapper {
    int insertShortUrl(ShortUrlVO shortUrl);

    ShortUrlVO findById(@Param("id") String id);

    ShortUrlTargetVO findClickTargetById(@Param("id") String id);

    ShortUrlTargetVO findTargetBySendTargetId(@Param("sendTargetId") Long sendTargetId);

    int markClicked(@Param("id") String id);

    int markFirstClicked(@Param("id") String id);

    int markConverted(@Param("id") String id);

    int incrementChannelClickTargetCount(@Param("channelId") Long channelId, @Param("actorId") Long actorId);

    int incrementChannelClickCount(@Param("channelId") Long channelId, @Param("actorId") Long actorId);

    int incrementHourlyClickCount(@Param("channelId") Long channelId, @Param("hour") int hour, @Param("actorId") Long actorId);
}
