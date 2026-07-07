package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.service.EmailMessageService;
import com.example.smartmessaging.util.MaskingUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * 이메일 발송 서비스 구현체 (AWS SES v2)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMessageServiceImpl implements EmailMessageService {

    private final SesV2Client sesV2Client;
    private final Validator validator;

    @Value("${app.dummy-send.enabled:false}")
    private boolean dummySendEnabled;

    @Value("${aws.ses.enabled:false}")
    private boolean sesEnabled;

    @Value("${aws.ses.from-email:}")
    private String fromEmail;

    @Value("${aws.ses.from-name:}")
    private String fromName;

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        Set<ConstraintViolation<EmailCheck>> violations = validator.validate(new EmailCheck(email));
        return violations.isEmpty();
    }

    private String encodeSenderName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        if (name.matches("\\A\\p{ASCII}*\\z")) {
            return name;
        }
        String base64 = Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + base64 + "?=";
    }

    @Override
    public SendResult sendEmail(String email, String title, String content) {
        log.info("[Email Service] Sending email to={}", MaskingUtils.maskEmail(email));

        if (!isValidEmail(email)) {
            return SendResult.fail("EMAIL", "INVALID_EMAIL", "이메일 주소 형식이 올바르지 않습니다.");
        }

        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return SendResult.fail("EMAIL", "INVALID_CONTENT", "제목과 본문은 필수입니다.");
        }

        if (dummySendEnabled) {
            log.info("[Email Service] Dummy mode enabled, returning success without actual sending.");
            return SendResult.success("EMAIL");
        }

        if (!sesEnabled) {
            return SendResult.fail("EMAIL", "NOT_IMPLEMENTED", "실제 이메일 발송 연동(AWS SES)이 비활성화되어 있습니다.");
        }

        if (!isValidEmail(fromEmail) || "no-reply@example.com".equals(fromEmail)) {
            return SendResult.fail("EMAIL", "INVALID_SENDER_EMAIL", "발신자 이메일 설정이 올바르지 않습니다.");
        }

        try {
            String encodedFromName = encodeSenderName(fromName);
            String source = encodedFromName != null && !encodedFromName.isBlank() 
                ? String.format("%s <%s>", encodedFromName, fromEmail) 
                : fromEmail;

            SendEmailRequest request = SendEmailRequest.builder()
                    .fromEmailAddress(source)
                    .destination(Destination.builder().toAddresses(email).build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder().data(title).charset("UTF-8").build())
                                    .body(Body.builder().text(Content.builder().data(content).charset("UTF-8").build()).build())
                                    .build())
                            .build())
                    .build();

            sesV2Client.sendEmail(request);
            log.info("[Email Service] Successfully sent email to={}", MaskingUtils.maskEmail(email));
            return SendResult.success("EMAIL");

        } catch (MessageRejectedException e) {
            log.error("[Email Service] Message rejected by SES for email={}", MaskingUtils.maskEmail(email), e);
            return SendResult.fail("EMAIL", "EMAIL_REJECTED", "이메일 발송이 거부되었습니다.");
        } catch (MailFromDomainNotVerifiedException e) {
            log.error("[Email Service] Sender domain not verified", e);
            return SendResult.fail("EMAIL", "SENDER_DOMAIN_NOT_VERIFIED", "발신자 도메인이 검증되지 않았습니다.");
        } catch (SendingPausedException e) {
            log.error("[Email Service] SES sending paused", e);
            return SendResult.fail("EMAIL", "EMAIL_SENDING_PAUSED", "AWS SES 계정 발송이 일시 중지되었습니다.");
        } catch (BadRequestException e) {
            log.error("[Email Service] Bad request to SES", e);
            return SendResult.fail("EMAIL", "EMAIL_BAD_REQUEST", "잘못된 이메일 발송 요청입니다.");
        } catch (TooManyRequestsException | LimitExceededException e) {
            log.warn("[Email Service] Throttling or Limit Exceeded for email={}", MaskingUtils.maskEmail(email), e);
            return SendResult.fail("EMAIL", "EMAIL_TEMPORARY_FAILURE", "일시적인 장애로 발송이 지연되었습니다.");
        } catch (NotFoundException e) {
            log.error("[Email Service] Sender or Identity not found/verified", e);
            return SendResult.fail("EMAIL", "SENDER_NOT_VERIFIED", "발신자 이메일이 검증되지 않았거나 존재하지 않습니다.");
        } catch (SesV2Exception e) {
            log.error("[Email Service] AWS SES V2 Exception occurred while sending to={}", MaskingUtils.maskEmail(email), e);
            return SendResult.fail("EMAIL", "EMAIL_SEND_FAIL", "이메일 발송 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("[Email Service] Unknown Exception occurred while sending to={}", MaskingUtils.maskEmail(email), e);
            return SendResult.fail("EMAIL", "EMAIL_SEND_FAIL", "시스템 오류로 이메일 발송에 실패했습니다.");
        }
    }
}
