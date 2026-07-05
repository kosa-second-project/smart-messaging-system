package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.MessageTaskDto;

public interface MessageQueuePublisher {
    void publish(MessageTaskDto task);
}
