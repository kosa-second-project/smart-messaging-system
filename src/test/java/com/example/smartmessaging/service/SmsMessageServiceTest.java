package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.vo.SendResult;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.model.MessageType;
import com.solapi.sdk.message.service.DefaultMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.example.smartmessaging.service.impl.SmsMessageServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SmsMessageServiceTest {

    private final DefaultMessageService solapiMessageService = mock(DefaultMessageService.class);

    @Test
    @DisplayName("SMS 채널이면 MessageType.SMS를 명시하고 자동 타입 판별을 끈다")
    void sendTextMessage_smsType() throws Exception {
        SmsMessageService service = new SmsMessageServiceImpl(
                solapiMessageService,
                true,
                "01012345678"
        );

        SendResult result = service.sendTextMessage("010-1111-2222", "제목", "짧은 문자", "SMS");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(solapiMessageService).send(captor.capture());
        Message message = captor.getValue();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("SMS");
        assertThat(message.getFrom()).isEqualTo("01012345678");
        assertThat(message.getTo()).isEqualTo("01011112222");
        assertThat(message.getText()).isEqualTo("짧은 문자");
        assertThat(message.getType()).isEqualTo(MessageType.SMS);
        assertThat(message.getAutoTypeDetect()).isFalse();
        assertThat(message.getSubject()).isNull();
    }

    @Test
    @DisplayName("LMS 채널이면 MessageType.LMS와 제목을 설정하고 자동 타입 판별을 끈다")
    void sendTextMessage_lmsType() throws Exception {
        SmsMessageService service = new SmsMessageServiceImpl(
                solapiMessageService,
                true,
                "01012345678"
        );

        SendResult result = service.sendTextMessage("01011112222", "공지", "긴 문자 본문", "LMS");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(solapiMessageService).send(captor.capture());
        Message message = captor.getValue();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("LMS");
        assertThat(message.getType()).isEqualTo(MessageType.LMS);
        assertThat(message.getAutoTypeDetect()).isFalse();
        assertThat(message.getSubject()).isEqualTo("공지");
    }

    @Test
    @DisplayName("수신번호가 올바르지 않으면 SOLAPI를 호출하지 않는다")
    void sendTextMessage_invalidPhone() throws Exception {
        SmsMessageService service = new SmsMessageServiceImpl(
                solapiMessageService,
                true,
                "01012345678"
        );

        SendResult result = service.sendTextMessage("123", "제목", "본문", "SMS");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getChannel()).isEqualTo("SMS");
        assertThat(result.getErrorCode()).isEqualTo("INVALID_PHONE_NUMBER");
        verify(solapiMessageService, never()).send(any(Message.class));
    }

    @Test
    @DisplayName("SOLAPI가 비활성화되어 있으면 SOLAPI를 호출하지 않는다")
    void sendTextMessage_disabled() throws Exception {
        SmsMessageService service = new SmsMessageServiceImpl(
                solapiMessageService,
                false,
                "01012345678"
        );

        SendResult result = service.sendTextMessage("01011112222", "제목", "본문", "SMS");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("SOLAPI_DISABLED");
        verify(solapiMessageService, never()).send(any(Message.class));
    }

    @Test
    @DisplayName("광고성 문자에는 추적 링크와 수신거부 링크를 본문에 추가한다")
    void sendTextMessage_adTextIncludesLinks() throws Exception {
        SmsMessageService service = new SmsMessageServiceImpl(
                solapiMessageService,
                true,
                "01012345678"
        );

        SendResult result = service.sendTextMessage(
                "01011112222",
                "광고",
                "쿠폰이 도착했습니다.",
                "SMS",
                "AD",
                "쿠폰 보기",
                "http://localhost:8080/r/abc",
                "http://localhost:8080/u/def"
        );

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(solapiMessageService).send(captor.capture());

        assertThat(result.isSuccess()).isTrue();
        assertThat(captor.getValue().getText())
                .contains("쿠폰 보기")
                .contains("http://localhost:8080/r/abc")
                .contains("수신거부를 원하시면")
                .contains("http://localhost:8080/u/def");
    }
}
