package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import java.util.List;

/**
 * 수신 대상 채널 결정 및 단가 계산 서비스 인터페이스
 */
public interface RecipientChannelResolver {
    List<RecipientSendPlan> resolve(
            List<SendRecipientCandidateVO> recipients,
            List<ChannelVO> activeChannels,
            List<String> requestedPriority
    );
}
