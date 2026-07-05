package com.example.smartmessaging.service;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.queue.MessageQueueDto;
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

    /**
     * RabbitMQ 메인 발송 대기열(message.send.queue)에서 일감을 선입선출(FIFO)로 꺼내와 비동기로 처리합니다.
     * 수동 ACK 모드를 가동하여, 실제 발송 로그 처리 및 DB 저장이 완벽히 성공해야만 큐에서 지웁니다.
     */
    @RabbitListener(queues = RabbitMQConfig.MAIN_QUEUE)
    public void consumeMessage(MessageQueueDto message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            // 가상 발송 처리 실행
            messageSender.send(message);
            
            // 수동 ACK: 정상 발송 성공을 큐에 보고 (메시지 큐에서 소멸)
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("[Queue Consumer] 메시지 처리 실패 - TargetID: {}, Error: {}", message.getSendTargetId(), e.getMessage());
            
            // 수동 NACK: requeue=false 설정을 통해 RabbitMQConfig에 설정된 DLQ(Dead Letter Queue)로 영구 유실 방지 격리
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
