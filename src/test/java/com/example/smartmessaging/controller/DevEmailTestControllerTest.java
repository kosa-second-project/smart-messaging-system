package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.service.EmailMessageService;
import com.example.smartmessaging.service.SmsMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevEmailTestControllerTest {

    private final EmailMessageService emailMessageService = mock(EmailMessageService.class);
    private final SmsMessageService smsMessageService = mock(SmsMessageService.class);
    private final DevEmailTestController controller = new DevEmailTestController(
            emailMessageService,
            smsMessageService
    );

    @Test
    @DisplayName("개발용 SMS 테스트 요청은 SmsMessageService에 채널 타입을 전달한다")
    void sendTestSms() {
        DevEmailTestController.SmsTestRequest request = new DevEmailTestController.SmsTestRequest(
                "010-1111-2222",
                "공지",
                "테스트 문자",
                "LMS"
        );
        SendResult expected = SendResult.success("LMS");
        when(smsMessageService.sendTextMessage("010-1111-2222", "공지", "테스트 문자", "LMS"))
                .thenReturn(expected);

        ResponseEntity<SendResult> response = controller.sendTestSms(request);

        assertThat(response.getBody()).isSameAs(expected);
        verify(smsMessageService).sendTextMessage("010-1111-2222", "공지", "테스트 문자", "LMS");
    }
}
