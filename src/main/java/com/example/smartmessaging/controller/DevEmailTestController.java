package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.service.EmailMessageService;
import com.example.smartmessaging.service.SmsMessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevEmailTestController {

    private final EmailMessageService emailMessageService;
    private final SmsMessageService smsMessageService;

    @PostMapping("/email-test")
    public ResponseEntity<SendResult> sendTestEmail(@Valid @RequestBody EmailTestRequest request) {
        SendResult result = emailMessageService.sendEmail(
                request.toEmail().trim(),
                request.title().trim(),
                request.content().trim()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sms-test")
    public ResponseEntity<SendResult> sendTestSms(@Valid @RequestBody SmsTestRequest request) {
        String channelType = request.channelType() == null || request.channelType().isBlank()
                ? "SMS"
                : request.channelType().trim().toUpperCase();
        SendResult result = smsMessageService.sendTextMessage(
                request.toPhone().trim(),
                request.title() == null ? null : request.title().trim(),
                request.content().trim(),
                channelType
        );
        return ResponseEntity.ok(result);
    }

    public record EmailTestRequest(
            @NotBlank @Email @Size(max = 320) String toEmail,
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 1000) String content
    ) {
    }

    public record SmsTestRequest(
            @NotBlank @Size(max = 20) String toPhone,
            @Size(max = 50) String title,
            @NotBlank @Size(max = 2000) String content,
            @Pattern(regexp = "SMS|LMS", flags = Pattern.Flag.CASE_INSENSITIVE) String channelType
    ) {
    }
}
