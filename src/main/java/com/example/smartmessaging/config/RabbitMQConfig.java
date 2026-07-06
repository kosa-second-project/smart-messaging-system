package com.example.smartmessaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CAMPAIGN_COMMAND_QUEUE = "campaign.command.queue";
    public static final String MESSAGE_SEND_QUEUE = "message.send.queue";
    public static final String DEAD_QUEUE = "message.dead.queue";

    @Bean
    public Queue campaignCommandQueue() {
        return QueueBuilder.durable(CAMPAIGN_COMMAND_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_QUEUE)
                .build();
    }

    @Bean
    public Queue messageSendQueue() {
        return QueueBuilder.durable(MESSAGE_SEND_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_QUEUE)
                .build();
    }

    @Bean
    public Queue messageDeadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
