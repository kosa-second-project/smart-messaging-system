package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.vo.ShortUrlVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShortUrlMapper {
    int insertShortUrl(ShortUrlVO shortUrl);

    ShortUrlVO findById(@Param("id") String id);

    int markClicked(@Param("id") String id);

    int markConverted(@Param("id") String id);
}
