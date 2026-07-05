package com.example.smartmessaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 1. 발송 처리용 메인 Exchange & Queue & Routing Key 정의
    public static final String MAIN_EXCHANGE = "message.send.exchange";
    public static final String MAIN_QUEUE = "message.send.queue";
    public static final String MAIN_ROUTING_KEY = "message.send.routing.key";

    // 2. 실패 메시지 격리용 Dead Letter (DLQ) Exchange & Queue & Routing Key 정의
    public static final String DLQ_EXCHANGE = "message.dead.exchange";
    public static final String DLQ_QUEUE = "message.dead.queue";
    public static final String DLQ_ROUTING_KEY = "message.dead.routing.key";

    // 2.5. 캠페인 벌크 발송 명령 전송용 Exchange & Queue & Routing Key 정의
    public static final String CAMP_COMMAND_EXCHANGE = "campaign.command.exchange";
    public static final String CAMP_COMMAND_QUEUE = "campaign.command.queue";
    public static final String CAMP_COMMAND_ROUTING_KEY = "campaign.command.routing.key";

    // 3. 메인 발송 대기열 큐 생성 (DLQ 바인딩 포함)
    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)       // 실패 시 DLQ 교환기로 배송
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY) // 실패 시 DLQ 라우팅 키 할당
                .build();
    }

    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE);
    }

    @Bean
    public Binding mainBinding() {
        return BindingBuilder.bind(mainQueue())
                .to(mainExchange())
                .with(MAIN_ROUTING_KEY);
    }

    // 4. Dead Letter Queue (DLQ) 생성 및 바인딩
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }

    // 4.5. 캠페인 벌크 발송 명령 큐 & 익스체인지 & 바인딩 등록
    @Bean
    public Queue campaignCommandQueue() {
        return QueueBuilder.durable(CAMP_COMMAND_QUEUE).build();
    }

    @Bean
    public DirectExchange campaignCommandExchange() {
        return new DirectExchange(CAMP_COMMAND_EXCHANGE);
    }

    @Bean
    public Binding campaignCommandBinding() {
        return BindingBuilder.bind(campaignCommandQueue())
                .to(campaignCommandExchange())
                .with(CAMP_COMMAND_ROUTING_KEY);
    }

    // 5. 메시지 전송 시 객체를 JSON 포맷으로 자동 변환해 주는 컨버터 등록
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
