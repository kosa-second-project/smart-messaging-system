package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.vo.ChannelVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChannelMapper {
    /**
     * 활성화된(is_active = 1) 채널 목록 조회
     */
    List<ChannelVO> getActiveChannels();
}
