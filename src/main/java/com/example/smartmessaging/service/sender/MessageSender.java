package com.example.smartmessaging.service.sender;

import com.example.smartmessaging.dto.model.MessageSendContext;
import com.example.smartmessaging.dto.model.MessageSendResult;

public interface MessageSender {
    MessageSendResult send(MessageSendContext context);
}
