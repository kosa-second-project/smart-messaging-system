package com.example.smartmessaging;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.queue.MessageQueueDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;

@SpringBootTest
public class MessageQueueLoadTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final Random random = new Random();

    @Test
    @Disabled("실제 통합 테스트 시에만 빌드 과정에서 수동 실행하도록 Disabled 처리")
    @DisplayName("RabbitMQ 대기열 10만 건 대량 메시지 적재(Enqueue) 비동기 성능 측정 테스트")
    public void test100kQueueLoadingBenchmark() {
        int count = 100000;
        System.out.println("🔥 [Benchmark] 가상 메시지 큐 대량 적재 테스트 시작 - 요청 건수: " + count + "건");
        
        long startTime = System.currentTimeMillis();
        String[] channels = {"KAKAO", "SMS", "EMAIL"};

        for (int i = 1; i <= count; i++) {
            String channel = channels[random.nextInt(channels.length)];
            String recipient = "010-8888-" + String.format("%04d", i % 10000);
            if ("EMAIL".equals(channel)) {
                recipient = "testuser" + i + "@example.com";
            }

            MessageQueueDto dto = MessageQueueDto.builder()
                    .sendTargetId((long) i) 
                    .customerId((long) (2000 + (i % 500)))
                    .recipientNo(recipient)
                    .channelType(channel)
                    .title(channel + " 대량 부하 테스트 마케팅 - " + i)
                    .content("안녕하세요 고객님! 본 메시지는 대기열 큐 비동기 발송 10만건 부하 분산 벤치마크 테스트 본문입니다. [" + i + "]")
                    .build();

            // RabbitMQ 메인 교환기로 적재 (비동기 버퍼링 가동)
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MAIN_EXCHANGE,
                    RabbitMQConfig.MAIN_ROUTING_KEY,
                    dto
            );
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("🎉 [Benchmark] 가상 메시지 큐 10만건 적재 완료!");
        System.out.println("⏳ 총 소요 시간: " + duration + " ms");
        System.out.println("⚡ 건당 평균 적재 시간: " + String.format("%.4f", (double) duration / count) + " ms");
    }
}
