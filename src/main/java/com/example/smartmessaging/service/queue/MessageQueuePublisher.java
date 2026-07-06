package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.queue.CampaignCommandQueueDto;
import com.example.smartmessaging.dto.queue.MessageQueueDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueuePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishCampaignCommand(CampaignCommandQueueDto command) {
        command.setType(CampaignCommandQueueDto.TYPE);
        rabbitTemplate.convertAndSend(RabbitMQConfig.CAMPAIGN_COMMAND_QUEUE, command);
    }

    public void publishCampaignCommandAfterCommit(CampaignCommandQueueDto command) {
        runAfterCommit(() -> publishCampaignCommand(command), null);
    }

    public void publishCampaignCommandAfterCommit(CampaignCommandQueueDto command, Consumer<RuntimeException> failureHandler) {
        runAfterCommit(() -> publishCampaignCommand(command), failureHandler);
    }

    public void publishMessageSend(MessageQueueDto message) {
        message.setType(MessageQueueDto.TYPE);
        rabbitTemplate.convertAndSend(RabbitMQConfig.MESSAGE_SEND_QUEUE, message);
    }

    public void publishMessageSendAfterCommit(MessageQueueDto message) {
        runAfterCommit(() -> publishMessageSend(message), null);
    }

    public void publishMessageSendAfterCommit(MessageQueueDto message, Consumer<RuntimeException> failureHandler) {
        runAfterCommit(() -> publishMessageSend(message), failureHandler);
    }

    private void runAfterCommit(Runnable action, Consumer<RuntimeException> failureHandler) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runNow(action, failureHandler);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (RuntimeException e) {
                    handleFailure(e, failureHandler);
                }
            }
        });
    }

    private void runNow(Runnable action, Consumer<RuntimeException> failureHandler) {
        try {
            action.run();
        } catch (RuntimeException e) {
            handleFailure(e, failureHandler);
            throw e;
        }
    }

    private void handleFailure(RuntimeException e, Consumer<RuntimeException> failureHandler) {
        if (failureHandler != null) {
            try {
                failureHandler.accept(e);
            } catch (RuntimeException handlerException) {
                e.addSuppressed(handlerException);
            }
        }
        log.error("RabbitMQ publish failed after database commit.", e);
    }
}
