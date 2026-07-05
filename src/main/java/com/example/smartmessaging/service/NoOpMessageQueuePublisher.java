package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.MessageTaskDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoOpMessageQueuePublisher implements MessageQueuePublisher {

    @Override
    public void publish(MessageTaskDto task) {
        log.info("[MessageQueuePublisher:NOOP] prepared sendHistoryId={}, sendTargetId={}, customerId={}, firstChannel={}",
                task.getSendHistoryId(),
                task.getSendTargetId(),
                task.getCustomerId(),
                task.getFallbackSequence() == null || task.getFallbackSequence().isEmpty()
                        ? "NONE"
                        : task.getFallbackSequence().get(0));
    }
}
