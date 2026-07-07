package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageConsumer {

    private final MessageSender messageSender;

    @RabbitListener(queues = RabbitMQConfig.MAIN_QUEUE)
    public void consumeMessage(MessageTaskDto task, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            messageSender.send(task);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MessageConsumer] system failure sendHistoryId={}, sendTargetId={}, error={}",
                    task.getSendHistoryId(), task.getSendTargetId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
