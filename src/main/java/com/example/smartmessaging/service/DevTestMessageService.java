package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.DevTestSendRequest;
import com.example.smartmessaging.dto.response.DevTestSendResponse;

public interface DevTestMessageService {
    DevTestSendResponse sendToMe(Long userId, String userName, DevTestSendRequest request);
}