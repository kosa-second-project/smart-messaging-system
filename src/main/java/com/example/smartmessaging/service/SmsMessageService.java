package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.vo.SendResult;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.model.MessageType;
import com.solapi.sdk.message.service.DefaultMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class SmsMessageService {

    private final DefaultMessageService solapiMessageService;
    private final boolean solapiEnabled;
    private final String fromNumber;
    private final String unsubscribeUrl;

    public SmsMessageService(
            DefaultMessageService solapiMessageService,
            @Value("${solapi.enabled:false}") boolean solapiEnabled,
            @Value("${solapi.from-number:}") String fromNumber,
            @Value("${messaging.unsubscribe-url:}") String unsubscribeUrl) {
        this.solapiMessageService = solapiMessageService;
        this.solapiEnabled = solapiEnabled;
        this.fromNumber = normalizePhoneNumber(fromNumber);
        this.unsubscribeUrl = unsubscribeUrl;
    }

    public SendResult sendTextMessage(String phoneNumber, String title, String content, String channelType, boolean advertising, String linkUrl) {
        String normalizedChannel = normalizeChannel(channelType);
        if (!solapiEnabled) {
            return SendResult.fail(normalizedChannel, "SOLAPI_DISABLED", "SOLAPI sending is disabled.");
        }
        if (fromNumber == null || fromNumber.isBlank()) {
            return SendResult.fail(normalizedChannel, "SOLAPI_CONFIG_ERROR", "SOLAPI sender number is not configured.");
        }
        String normalizedTo = normalizePhoneNumber(phoneNumber);
        if (!isValidRecipientPhoneNumber(normalizedTo)) {
            return SendResult.fail(normalizedChannel, "INVALID_PHONE_NUMBER", "Recipient phone number is invalid.");
        }
        if (content == null || content.isBlank()) {
            return SendResult.fail(normalizedChannel, "INVALID_CONTENT", "Message content is required.");
        }
        if (advertising && isBlank(unsubscribeUrl)) {
            return SendResult.fail(normalizedChannel, "MISSING_UNSUBSCRIBE_URL", "Advertising SMS requires an unsubscribe URL.");
        }

        try {
            Message message = new Message();
            message.setFrom(fromNumber);
            message.setTo(normalizedTo);
            message.setText(buildText(content, advertising, linkUrl));
            message.setType(toMessageType(normalizedChannel));
            message.setAutoTypeDetect(false);
            if ("LMS".equals(normalizedChannel) && title != null && !title.isBlank()) {
                message.setSubject(title.trim());
            }

            solapiMessageService.send(message);
            return SendResult.success(normalizedChannel, "SOLAPI-" + UUID.randomUUID());
        } catch (SolapiMessageNotReceivedException e) {
            log.warn("SOLAPI did not receive {} message to={}", normalizedChannel, normalizedTo, e);
            return SendResult.fail(normalizedChannel, "SOLAPI_NOT_RECEIVED", "SOLAPI did not receive the message.");
        } catch (Exception e) {
            log.error("SOLAPI send failed. channel={}, to={}", normalizedChannel, normalizedTo, e);
            return SendResult.fail(normalizedChannel, "SMS_SEND_FAIL", "SOLAPI send failed.");
        }
    }

    private String buildText(String content, boolean advertising, String linkUrl) {
        StringBuilder text = new StringBuilder(content.trim());
        if (!isBlank(linkUrl)) {
            text.append("\n\n").append(linkUrl.trim());
        }
        if (advertising) {
            text.append("\n\n수신거부: ").append(unsubscribeUrl.trim());
        }
        return text.toString();
    }

    private MessageType toMessageType(String channelType) {
        return "LMS".equals(channelType) ? MessageType.LMS : MessageType.SMS;
    }

    private String normalizeChannel(String channelType) {
        if (channelType == null) {
            return "SMS";
        }
        String normalized = channelType.trim().toUpperCase(Locale.ROOT);
        return "LMS".equals(normalized) ? "LMS" : "SMS";
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
