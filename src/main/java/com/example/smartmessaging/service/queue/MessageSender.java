package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.dto.request.MessageTaskDto;

public interface MessageSender {
    void send(MessageTaskDto task);
}
