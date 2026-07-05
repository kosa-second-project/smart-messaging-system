package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.vo.SendResult;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailMessageServiceTest {

    @Mock
    private SesV2Client sesV2Client;

    @Spy
    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @InjectMocks
    private EmailMessageService emailMessageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailMessageService, "fromEmail", "valid-sender@test.com");
        ReflectionTestUtils.setField(emailMessageService, "fromName", "Test Sender");
    }

    @Test
    @DisplayName("Dummy 모드가 켜져있으면 SES 호출 없이 성공을 반환한다")
    void sendEmail_dummyEnabled() {
        ReflectionTestUtils.setField(emailMessageService, "dummySendEnabled", true);

        SendResult result = emailMessageService.sendEmail("test@test.com", "Title", "Content");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("EMAIL");
        verify(sesV2Client, never()).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    @DisplayName("SES가 비활성화되어 있으면 실패(NOT_IMPLEMENTED)를 반환한다")
    void sendEmail_sesDisabled() {
        ReflectionTestUtils.setField(emailMessageService, "dummySendEnabled", false);
        ReflectionTestUtils.setField(emailMessageService, "sesEnabled", false);

        SendResult result = emailMessageService.sendEmail("test@test.com", "Title", "Content");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("NOT_IMPLEMENTED");
        verify(sesV2Client, never()).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    @DisplayName("발신자 이메일이 잘못 설정되어 있으면 실패(INVALID_SENDER_EMAIL)를 반환한다")
    void sendEmail_invalidSenderEmail() {
        ReflectionTestUtils.setField(emailMessageService, "dummySendEnabled", false);
        ReflectionTestUtils.setField(emailMessageService, "sesEnabled", true);

        // test default placeholder
        ReflectionTestUtils.setField(emailMessageService, "fromEmail", "no-reply@example.com");
        SendResult result1 = emailMessageService.sendEmail("test@test.com", "Title", "Content");
        assertThat(result1.isSuccess()).isFalse();
        assertThat(result1.getErrorCode()).isEqualTo("INVALID_SENDER_EMAIL");

        // test invalid email
        ReflectionTestUtils.setField(emailMessageService, "fromEmail", "invalid");
        SendResult result2 = emailMessageService.sendEmail("test@test.com", "Title", "Content");
        assertThat(result2.isSuccess()).isFalse();
        assertThat(result2.getErrorCode()).isEqualTo("INVALID_SENDER_EMAIL");
    }

    @Test
    @DisplayName("올바른 요청이면 SES를 호출하고 UTF-8 캐릭터셋과 정상 인코딩된 이름을 사용한다")
    void sendEmail_success() {
        ReflectionTestUtils.setField(emailMessageService, "dummySendEnabled", false);
        ReflectionTestUtils.setField(emailMessageService, "sesEnabled", true);
        ReflectionTestUtils.setField(emailMessageService, "fromName", "스마트메시징");

        SendEmailResponse mockResponse = SendEmailResponse.builder().messageId("msg-123").build();
        when(sesV2Client.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        SendResult result = emailMessageService.sendEmail("test@test.com", "Title", "Content");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("EMAIL");

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesV2Client, times(1)).sendEmail(captor.capture());
        
        SendEmailRequest request = captor.getValue();
        // Base64 encoding of "스마트메시징" is "7Iqk66eI7Yq466mU7Iuc7KeV"
        assertThat(request.fromEmailAddress()).isEqualTo("=?UTF-8?B?7Iqk66eI7Yq466mU7Iuc7KeV?= <valid-sender@test.com>");
        assertThat(request.destination().toAddresses()).containsExactly("test@test.com");
        
        Content subject = request.content().simple().subject();
        assertThat(subject.data()).isEqualTo("Title");
        assertThat(subject.charset()).isEqualTo("UTF-8");
        
        Content body = request.content().simple().body().text();
        assertThat(body.data()).isEqualTo("Content");
        assertThat(body.charset()).isEqualTo("UTF-8");
    }

    @Test
    @DisplayName("ASCII 발신자 이름은 인코딩하지 않는다")
    void sendEmail_success_asciiName() {
        ReflectionTestUtils.setField(emailMessageService, "dummySendEnabled", false);
        ReflectionTestUtils.setField(emailMessageService, "sesEnabled", true);
        ReflectionTestUtils.setField(emailMessageService, "fromName", "Smart Messaging");

        SendEmailResponse mockResponse = SendEmailResponse.builder().messageId("msg-123").build();
        when(sesV2Client.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        emailMessageService.sendEmail("test@test.com", "Title", "Content");

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesV2Client, times(1)).sendEmail(captor.capture());
        assertThat(captor.getValue().fromEmailAddress()).isEqualTo("Smart Messaging <valid-sender@test.com>");
    }

    @Test
    @DisplayName("이메일 주소 형식이 올바르지 않으면 실패(INVALID_EMAIL)를 반환한다")
    void sendEmail_invalidEmail() {
        ReflectionTestUtils.setField(emailMessageService, "dummySendEnabled", false);
        ReflectionTestUtils.setField(emailMessageService, "sesEnabled", true);

        SendResult result = emailMessageService.sendEmail("invalid-email", "Title", "Content");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("INVALID_EMAIL");

        SendResult result2 = emailMessageService.sendEmail("test.com", "Title", "Content");
        assertThat(result2.getErrorCode()).isEqualTo("INVALID_EMAIL");

        SendResult result3 = emailMessageService.sendEmail("a@", "Title", "Content");
        assertThat(result3.getErrorCode()).isEqualTo("INVALID_EMAIL");
    }

    @Test
    @DisplayName("SES 예외 매핑 확인")
    void sendEmail_exceptionMapping() {
        ReflectionTestUtils.setField(emailMessageService, "dummySendEnabled", false);
        ReflectionTestUtils.setField(emailMessageService, "sesEnabled", true);

        // MessageRejected
        when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(MessageRejectedException.builder().message("Rejected").build());
        assertThat(emailMessageService.sendEmail("test@test.com", "T", "C").getErrorCode()).isEqualTo("EMAIL_REJECTED");

        // DomainNotVerified
        when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(MailFromDomainNotVerifiedException.builder().message("Not Verified").build());
        assertThat(emailMessageService.sendEmail("test@test.com", "T", "C").getErrorCode()).isEqualTo("SENDER_DOMAIN_NOT_VERIFIED");

        // SendingPaused
        when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(SendingPausedException.builder().message("Paused").build());
        assertThat(emailMessageService.sendEmail("test@test.com", "T", "C").getErrorCode()).isEqualTo("EMAIL_SENDING_PAUSED");

        // BadRequest
        when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(BadRequestException.builder().message("Bad").build());
        assertThat(emailMessageService.sendEmail("test@test.com", "T", "C").getErrorCode()).isEqualTo("EMAIL_BAD_REQUEST");

        // TooManyRequests
        when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(TooManyRequestsException.builder().message("Throttling").build());
        assertThat(emailMessageService.sendEmail("test@test.com", "T", "C").getErrorCode()).isEqualTo("EMAIL_TEMPORARY_FAILURE");
    }
}
