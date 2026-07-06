package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.mapper.ChannelMapper;
import com.example.smartmessaging.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 채널 서비스 구현체
 */
@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelMapper channelMapper;

    @Override
    public List<ChannelVO> getActiveChannels() {
        return channelMapper.getActiveChannels();
    }
}
