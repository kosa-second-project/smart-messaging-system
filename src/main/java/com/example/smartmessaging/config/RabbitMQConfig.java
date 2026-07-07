package com.example.smartmessaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MAIN_EXCHANGE = "message.send.exchange";
    public static final String MAIN_QUEUE = "message.send.queue";
    public static final String MAIN_ROUTING_KEY = "message.send.routing.key";

    public static final String DLQ_EXCHANGE = "message.dead.exchange";
    public static final String DLQ_QUEUE = "message.dead.queue";
    public static final String DLQ_ROUTING_KEY = "message.dead.routing.key";

    public static final String CAMP_COMMAND_EXCHANGE = "campaign.command.exchange";
    public static final String CAMP_COMMAND_QUEUE = "campaign.command.queue";
    public static final String CAMP_COMMAND_ROUTING_KEY = "campaign.command.routing.key";


    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
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

    @Bean
    public Queue campaignCommandQueue() {
        return QueueBuilder.durable(CAMP_COMMAND_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
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

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
