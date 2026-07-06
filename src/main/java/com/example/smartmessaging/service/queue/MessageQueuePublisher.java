package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.queue.CampaignCommandQueueDto;
import com.example.smartmessaging.dto.queue.MessageQueueDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class MessageQueuePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishCampaignCommand(CampaignCommandQueueDto command) {
        command.setType(CampaignCommandQueueDto.TYPE);
        rabbitTemplate.convertAndSend(RabbitMQConfig.CAMPAIGN_COMMAND_QUEUE, command);
    }

    public void publishCampaignCommandAfterCommit(CampaignCommandQueueDto command) {
        runAfterCommit(() -> publishCampaignCommand(command));
    }

    public void publishMessageSend(MessageQueueDto message) {
        message.setType(MessageQueueDto.TYPE);
        rabbitTemplate.convertAndSend(RabbitMQConfig.MESSAGE_SEND_QUEUE, message);
    }

    public void publishMessageSendAfterCommit(MessageQueueDto message) {
        runAfterCommit(() -> publishMessageSend(message));
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
