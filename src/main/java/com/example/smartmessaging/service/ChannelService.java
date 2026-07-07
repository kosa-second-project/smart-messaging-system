package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.vo.ChannelVO;
import java.util.List;

/**
 * 채널 서비스 인터페이스
 */
public interface ChannelService {
    List<ChannelVO> getActiveChannels();
}
