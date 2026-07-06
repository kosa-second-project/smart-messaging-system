package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.vo.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EmailMessageService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final boolean dummyEnabled;

    public EmailMessageService(@Value("${messaging.email.dummy-enabled:false}") boolean dummyEnabled) {
        this.dummyEnabled = dummyEnabled;
    }

    public SendResult sendEmail(String email, String title, String content, String linkUrl) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return SendResult.fail("EMAIL", "INVALID_EMAIL", "Recipient email is invalid.");
        }
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return SendResult.fail("EMAIL", "INVALID_CONTENT", "Email title and content are required.");
        }
        if (!dummyEnabled) {
            return SendResult.fail("EMAIL", "EMAIL_PROVIDER_DISABLED", "Email provider is not configured.");
        }

        log.info("Dummy email send accepted. to={}, linkUrlPresent={}", maskEmail(email), linkUrl != null && !linkUrl.isBlank());
        return SendResult.success("EMAIL", "EMAIL-MOCK-" + UUID.randomUUID());
    }

    private String maskEmail(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
