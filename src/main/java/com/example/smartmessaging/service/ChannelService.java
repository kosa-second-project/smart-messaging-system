package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.mapper.ChannelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelMapper channelMapper;

    /**
     * 활성화된(is_active = 1) 채널 목록 조회
     */
    public List<ChannelVO> getActiveChannels() {
        return channelMapper.getActiveChannels();
    }
}
