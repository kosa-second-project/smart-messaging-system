package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendAttemptVO;
import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.service.ChannelService;
import com.example.smartmessaging.service.MessageRouterService;
import com.example.smartmessaging.service.repository.HistoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerMessageSenderImplTest {

    @Mock
    private HistoryMapper historyMapper;

    @Mock
    private ChannelService channelService;

    @Mock
    private MessageRouterService messageRouterService;

    @InjectMocks
    private CustomerMessageSenderImpl sender;

    @Test
    void fakeCustomer_isSavedAsMockSuccessWithoutCallingRealSender() {
        MessageTaskDto task = MessageTaskDto.builder()
                .messageId("send-10-20")
                .sendHistoryId(10L)
                .sendTargetId(20L)
                .customerId(30L)
                .userId(40L)
                .isRealCustomer(false)
                .phoneNumber("01011112222")
                .fallbackSequence(List.of("SMS"))
                .currentStep(0)
                .build();

        when(channelService.getActiveChannels()).thenReturn(List.of(channel(1L, "SMS")));

        sender.send(task);

        ArgumentCaptor<SendAttemptVO> attemptCaptor = ArgumentCaptor.forClass(SendAttemptVO.class);
        verify(historyMapper).insertSendAttempt(attemptCaptor.capture());
        SendAttemptVO attempt = attemptCaptor.getValue();

        assertThat(attempt.getSendTargetId()).isEqualTo(20L);
        assertThat(attempt.getChannelId()).isEqualTo(1L);
        assertThat(attempt.getIsSucceeded()).isTrue();
        assertThat(attempt.getSolapiMessageId()).isEqualTo("MOCK-send-10-20");

        verify(messageRouterService, never()).send(any());
        verify(historyMapper).updateSendTargetStatus(20L, "SENDING");
        verify(historyMapper).updateSendTargetSuccess(20L, 1L);
    }

    @Test
    void realCustomer_successAddsActualCostForSucceededChannel() {
        MessageTaskDto task = MessageTaskDto.builder()
                .messageId("send-10-20")
                .sendHistoryId(10L)
                .sendTargetId(20L)
                .customerId(30L)
                .userId(40L)
                .isRealCustomer(true)
                .phoneNumber("01011112222")
                .fallbackSequence(List.of("SMS"))
                .currentStep(0)
                .build();

        when(channelService.getActiveChannels()).thenReturn(List.of(channel(1L, "SMS")));
        when(messageRouterService.send(any())).thenReturn(SendResult.success("SMS"));

        sender.send(task);

        verify(historyMapper).updateSendTargetSuccess(20L, 1L);
    }

    private ChannelVO channel(Long id, String type) {
        return ChannelVO.builder()
                .id(id)
                .channelType(type)
                .costPerMsg(BigDecimal.ZERO)
                .isActive(true)
                .build();
    }
}
