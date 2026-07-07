package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.KakaoFeedMessageRequest;

public interface SendQueueService {
    /**
     * 카카오톡 발송 요청을 받아 DB에 이력을 생성하고 대기열 큐에 일괄 적재합니다.
     */
    void queueKakaoMessages(Long userId, String creator, KakaoFeedMessageRequest request, String kakaoAccessToken);

    /**
     * 일반 DB 기반 대량 캠페인 발송 요청을 받아 DB 이력을 남기고 비동기 큐에 적재합니다.
     */
    void queueCampaignMessages(Long userId, String draftId, com.example.smartmessaging.dto.request.MessageSendRequestDTO request, String kakaoAccessToken);
}

