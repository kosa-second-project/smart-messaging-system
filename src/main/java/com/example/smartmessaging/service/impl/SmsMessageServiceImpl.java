package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.service.SmsMessageService;
import com.example.smartmessaging.util.MaskingUtils;
import com.example.smartmessaging.util.SmsMessageTypeResolver;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.model.MessageType;
import com.solapi.sdk.message.service.DefaultMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * SMS/LMS 문자 발송 서비스 구현체 (SOLAPI 연동)
 */
@Slf4j
@Service
public class SmsMessageServiceImpl implements SmsMessageService {

    private final DefaultMessageService solapiMessageService;
    private final boolean solapiEnabled;
    private final String fromNumber;

    public SmsMessageServiceImpl(
            DefaultMessageService solapiMessageService,
            @Value("${solapi.enabled:false}") boolean solapiEnabled,
            @Value("${solapi.from-number:}") String fromNumber
    ) {
        this.solapiMessageService = solapiMessageService;
        this.solapiEnabled = solapiEnabled;
        this.fromNumber = normalizePhoneNumber(fromNumber);
    }

    @Override
    public SendResult sendSms(String phoneNumber, String content) {
        return sendTextMessage(phoneNumber, null, content, "SMS");
    }

    @Override
    public SendResult sendTextMessage(String phoneNumber, String title, String content, String channelType) {
        return sendTextMessage(phoneNumber, title, content, channelType, null, null, null, null);
    }

    @Override
    public SendResult sendTextMessage(
            String phoneNumber,
            String title,
            String content,
            String channelType,
            String purpose,
            String actionButtonName,
            String actionUrl,
            String unsubscribeUrl
    ) {
        String normalizedChannel = normalizeChannel(channelType);
        log.info("[SMS Service] Sending {} message to={}", normalizedChannel, MaskingUtils.maskPhone(phoneNumber));

        if (!solapiEnabled) {
            return SendResult.fail(normalizedChannel, "SOLAPI_DISABLED", "SOLAPI 문자 발송이 비활성화되어 있습니다.");
        }

        if (fromNumber == null || fromNumber.isBlank()) {
            return SendResult.fail(normalizedChannel, "SOLAPI_CONFIG_ERROR", "SOLAPI 발신번호 설정이 올바르지 않습니다.");
        }

        String normalizedTo = normalizePhoneNumber(phoneNumber);
        if (!isValidRecipientPhoneNumber(normalizedTo)) {
            return SendResult.fail(normalizedChannel, "INVALID_PHONE_NUMBER", "수신번호 형식이 올바르지 않습니다.");
        }

        if (content == null || content.isBlank()) {
            return SendResult.fail(normalizedChannel, "INVALID_CONTENT", "문자 본문은 필수입니다.");
        }

        if (isAdvertising(purpose) && isBlank(unsubscribeUrl)) {
            return SendResult.fail(normalizedChannel, "MISSING_UNSUBSCRIBE_URL", "광고성 문자에는 수신거부 링크가 필요합니다.");
        }

        String messageText = SmsMessageTypeResolver.buildMessageText(content, purpose, actionButtonName, actionUrl, unsubscribeUrl);

        try {
            Message message = new Message();
            message.setFrom(fromNumber);
            message.setTo(normalizedTo);
            message.setText(messageText);
            message.setType(toMessageType(normalizedChannel));
            message.setAutoTypeDetect(false);

            if ("LMS".equals(normalizedChannel) && title != null && !title.isBlank()) {
                message.setSubject(title.trim());
            }

            solapiMessageService.send(message);
            log.info("[SMS Service] Successfully sent {} message to={}",
                    normalizedChannel, MaskingUtils.maskPhone(normalizedTo));
            return SendResult.success(normalizedChannel);
        } catch (SolapiMessageNotReceivedException e) {
            log.warn("[SMS Service] SOLAPI did not receive message. Failed messages list: {}", e.getFailedMessageList());
            log.warn("[SMS Service] SOLAPI did not receive message channel={}, to={}, failedCount={}",
                    normalizedChannel,
                    MaskingUtils.maskPhone(normalizedTo),
                    e.getFailedMessageList() == null ? 0 : e.getFailedMessageList().size(),
                    e);
            return SendResult.fail(normalizedChannel, "SOLAPI_NOT_RECEIVED", "SOLAPI 문자 발송 접수에 실패했습니다.");
        } catch (Exception e) {
            log.error("[SMS Service] Failed to send {} message to={}",
                    normalizedChannel, MaskingUtils.maskPhone(normalizedTo), e);
            return SendResult.fail(normalizedChannel, "SMS_SEND_FAIL", "문자 발송 중 오류가 발생했습니다.");
        }
    }

    private MessageType toMessageType(String channelType) {
        if ("LMS".equals(channelType)) {
            return MessageType.LMS;
        }
        return MessageType.SMS;
    }

    private String normalizeChannel(String channelType) {
        if (channelType == null) {
            return "SMS";
        }
        String normalized = channelType.trim().toUpperCase(Locale.ROOT);
        if ("LMS".equals(normalized)) {
            return "LMS";
        }
        return "SMS";
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    private boolean isValidRecipientPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("\\d{10,11}");
    }

    private boolean isAdvertising(String purpose) {
        return "AD".equals(normalizePurpose(purpose));
    }

    private String normalizePurpose(String purpose) {
        if (purpose == null) {
            return "";
        }
        String normalized = purpose.trim().toUpperCase(Locale.ROOT);
        if ("INFORMATIONAL".equals(normalized)) {
            return "INFO";
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
