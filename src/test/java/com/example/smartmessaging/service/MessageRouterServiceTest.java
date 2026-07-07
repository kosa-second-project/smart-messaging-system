package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.vo.SendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageRouterServiceTest {

    @Mock
    private KakaoMessageService kakaoMessageService;

    @Mock
    private SmsMessageService smsMessageService;

    @Mock
    private EmailMessageService emailMessageService;

    @InjectMocks
    private com.example.smartmessaging.service.impl.MessageRouterServiceImpl messageRouterService;

    @Test
    @DisplayName("EMAIL 채널이면 EmailMessageService.sendEmail을 호출한다")
    void send_emailChannel() {
        // given
        MessageTaskDto task = new MessageTaskDto();
        task.setMessageId("msg-123");
        task.setFallbackSequence(List.of("EMAIL"));
        task.setCurrentStep(0);
        task.setEmail("test@test.com");
        task.setTitle("Test Title");
        task.setContent("Test Content");

        SendResult expectedResult = SendResult.success("EMAIL");
        when(emailMessageService.sendEmail("test@test.com", "Test Title", "Test Content"))
                .thenReturn(expectedResult);

        // when
        SendResult result = messageRouterService.send(task);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("EMAIL");
        verify(emailMessageService, times(1)).sendEmail("test@test.com", "Test Title", "Test Content");
        verifyNoInteractions(kakaoMessageService);
        verifyNoInteractions(smsMessageService);
    }

    @Test
    @DisplayName("SMS 채널이면 SmsMessageService에 SMS 타입으로 라우팅한다")
    void send_smsChannel() {
        // given
        MessageTaskDto task = new MessageTaskDto();
        task.setMessageId("msg-123");
        task.setFallbackSequence(List.of("SMS"));
        task.setCurrentStep(0);
        task.setPhoneNumber("01011112222");
        task.setTitle("");
        task.setContent("Test Content");

        SendResult expectedResult = SendResult.success("SMS");
        when(smsMessageService.sendTextMessage("01011112222", "", "Test Content", "SMS", null, null, null, null))
                .thenReturn(expectedResult);

        // when
        SendResult result = messageRouterService.send(task);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("SMS");
        verify(smsMessageService, times(1))
                .sendTextMessage("01011112222", "", "Test Content", "SMS", null, null, null, null);
        verifyNoInteractions(kakaoMessageService);
        verifyNoInteractions(emailMessageService);
    }

    @Test
    @DisplayName("SMS 채널이어도 제목이 있으면 LMS 타입으로 보정한다")
    void send_smsChannelWithTitleRoutesAsLms() {
        MessageTaskDto task = new MessageTaskDto();
        task.setMessageId("msg-123");
        task.setFallbackSequence(List.of("SMS"));
        task.setCurrentStep(0);
        task.setPhoneNumber("01011112222");
        task.setTitle("Test Title");
        task.setContent("Test Content");

        SendResult expectedResult = SendResult.success("LMS");
        when(smsMessageService.sendTextMessage("01011112222", "Test Title", "Test Content", "LMS", null, null, null, null))
                .thenReturn(expectedResult);

        SendResult result = messageRouterService.send(task);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("LMS");
        verify(smsMessageService, times(1))
                .sendTextMessage("01011112222", "Test Title", "Test Content", "LMS", null, null, null, null);
        verifyNoInteractions(kakaoMessageService);
        verifyNoInteractions(emailMessageService);
    }

    @Test
    @DisplayName("SMS 채널이어도 본문이 SMS 기준 바이트를 넘으면 LMS 타입으로 보정한다")
    void send_smsChannelLongContentRoutesAsLms() {
        // given
        String longContent = "가".repeat(46);
        MessageTaskDto task = new MessageTaskDto();
        task.setMessageId("msg-123");
        task.setFallbackSequence(List.of("SMS"));
        task.setCurrentStep(0);
        task.setPhoneNumber("01011112222");
        task.setTitle("Test Title");
        task.setContent(longContent);

        SendResult expectedResult = SendResult.success("LMS");
        when(smsMessageService.sendTextMessage("01011112222", "Test Title", longContent, "LMS", null, null, null, null))
                .thenReturn(expectedResult);

        // when
        SendResult result = messageRouterService.send(task);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("LMS");
        verify(smsMessageService, times(1))
                .sendTextMessage("01011112222", "Test Title", longContent, "LMS", null, null, null, null);
        verifyNoInteractions(kakaoMessageService);
        verifyNoInteractions(emailMessageService);
    }

    @Test
    @DisplayName("LMS 채널이면 SmsMessageService에 LMS 타입으로 라우팅한다")
    void send_lmsChannel() {
        // given
        MessageTaskDto task = new MessageTaskDto();
        task.setMessageId("msg-123");
        task.setFallbackSequence(List.of("LMS"));
        task.setCurrentStep(0);
        task.setPhoneNumber("01011112222");
        task.setTitle("Test Title");
        task.setContent("Long Test Content");

        SendResult expectedResult = SendResult.success("LMS");
        when(smsMessageService.sendTextMessage("01011112222", "Test Title", "Long Test Content", "LMS", null, null, null, null))
                .thenReturn(expectedResult);

        // when
        SendResult result = messageRouterService.send(task);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("LMS");
        verify(smsMessageService, times(1))
                .sendTextMessage("01011112222", "Test Title", "Long Test Content", "LMS", null, null, null, null);
        verifyNoInteractions(kakaoMessageService);
        verifyNoInteractions(emailMessageService);
    }
}
