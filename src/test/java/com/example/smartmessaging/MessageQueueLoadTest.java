package com.example.smartmessaging;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.request.MessageTaskDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Random;

@SpringBootTest
@Disabled("Manual benchmark only")
public class MessageQueueLoadTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final Random random = new Random();

    @Test
    @DisplayName("RabbitMQ message.send.queue 100k enqueue benchmark")
    public void test100kQueueLoadingBenchmark() {
        int count = 100000;
        long startTime = System.currentTimeMillis();
        String[] channels = {"KAKAO", "SMS", "EMAIL"};

        for (int i = 1; i <= count; i++) {
            String channel = channels[random.nextInt(channels.length)];
            MessageTaskDto task = MessageTaskDto.builder()
                    .messageId("bench-" + i)
                    .sendHistoryId(1L)
                    .sendTargetId((long) i)
                    .customerId((long) (2000 + (i % 500)))
                    .phoneNumber("010-8888-" + String.format("%04d", i % 10000))
                    .email("testuser" + i + "@example.com")
                    .kakaoUserKey("kakao-user-" + i)
                    .title(channel + " load test - " + i)
                    .content("RabbitMQ async message benchmark body " + i)
                    .purpose("INFO")
                    .fallbackSequence(List.of(channel))
                    .currentStep(0)
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MAIN_EXCHANGE,
                    RabbitMQConfig.MAIN_ROUTING_KEY,
                    task
            );
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Published " + count + " messages in " + duration + " ms");
    }
}
