package com.example.smartmessaging.service.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueMonitorService {

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final List<Queue> queues;

    public long messageCount(String queueName) {
        try {
            declareKnownQueue(queueName);
            Properties properties = amqpAdmin.getQueueProperties(queueName);
            if (properties != null) {
                return numberValue(properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT));
            }
            return passiveMessageCount(queueName);
        } catch (RuntimeException e) {
            log.warn("Unable to read RabbitMQ queue depth. queueName={}", queueName, e);
            return -1L;
        }
    }

    private void declareKnownQueue(String queueName) {
        Queue queue = queueDefinitions().get(queueName);
        if (queue != null) {
            amqpAdmin.declareQueue(queue);
        }
    }

    private Map<String, Queue> queueDefinitions() {
        return queues.stream()
                .collect(Collectors.toMap(Queue::getName, Function.identity(), (left, right) -> left));
    }

    private long passiveMessageCount(String queueName) {
        Integer count = rabbitTemplate.execute(channel -> channel.queueDeclarePassive(queueName).getMessageCount());
        return count == null ? 0L : count;
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
