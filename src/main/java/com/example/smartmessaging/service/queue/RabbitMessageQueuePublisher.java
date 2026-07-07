package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.request.MessageTaskDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMessageQueuePublisher implements MessageQueuePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(MessageTaskDto task) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MAIN_EXCHANGE,
                RabbitMQConfig.MAIN_ROUTING_KEY,
                task
        );
        log.info("[MessageQueuePublisher] published sendHistoryId={}, sendTargetId={}, customerId={}, firstChannel={}",
                task.getSendHistoryId(),
                task.getSendTargetId(),
                task.getCustomerId(),
                task.getFallbackSequence() == null || task.getFallbackSequence().isEmpty()
                        ? "NONE"
                        : task.getFallbackSequence().get(0));
    }
}
