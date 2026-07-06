package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.dto.request.MessageTaskDto;

public interface MessageQueuePublisher {
    void publish(MessageTaskDto task);
}
