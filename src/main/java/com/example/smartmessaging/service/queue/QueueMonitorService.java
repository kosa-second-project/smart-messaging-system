package com.example.smartmessaging.service.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueMonitorService {

    private final RabbitTemplate rabbitTemplate;

    public long messageCount(String queueName) {
        try {
            Integer count = rabbitTemplate.execute(channel -> channel.queueDeclarePassive(queueName).getMessageCount());
            return count == null ? 0L : count;
        } catch (Exception e) {
            return -1L;
        }
    }
}
