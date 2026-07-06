package com.example.smartmessaging.service.sender;

import com.example.smartmessaging.dto.model.MessageSendContext;
import com.example.smartmessaging.dto.model.MessageSendResult;
import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.service.EmailMessageService;
import com.example.smartmessaging.service.KakaoQueueMessageService;
import com.example.smartmessaging.service.SmsMessageService;
import com.example.smartmessaging.service.repository.SendQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WhitelistAwareMessageSender implements MessageSender {

    private final SendQueueRepository sendQueueRepository;
    private final EmailMessageService emailMessageService;
    private final KakaoQueueMessageService kakaoQueueMessageService;
    private final SmsMessageService smsMessageService;

    @Override
    public MessageSendResult send(MessageSendContext context) {
        if (context.getRecipientValue() == null || context.getRecipientValue().isBlank()) {
            return MessageSendResult.failure("MISSING_RECIPIENT_VALUE");
        }

        if (!isWhitelisted(context)) {
            return MessageSendResult.success(true, "MOCK-" + UUID.randomUUID());
        }

        SendResult result = switch (normalize(context.getChannelType())) {
            case "SMS", "LMS" -> smsMessageService.sendTextMessage(
                    context.getRecipientValue(),
                    context.getTitle(),
                    context.getContent(),
                    context.getChannelType(),
                    context.isAdvertising(),
                    context.getLinkUrl());
            case "EMAIL" -> emailMessageService.sendEmail(
                    context.getRecipientValue(),
                    context.getTitle(),
                    context.getContent(),
                    context.getLinkUrl());
            case "KAKAO" -> kakaoQueueMessageService.sendKakao(
                    context.getRecipientValue(),
                    context.getTitle(),
                    context.getContent(),
                    context.getLinkUrl());
            default -> SendResult.fail(
                    normalize(context.getChannelType()),
                    "UNSUPPORTED_CHANNEL",
                    "Unsupported channel type.");
        };

        if (result.isSuccess()) {
            return MessageSendResult.success(false, result.getProviderMessageId());
        }
        return MessageSendResult.failure(result.getErrorCode() + ":" + result.getErrorMessage());
    }

    private boolean isWhitelisted(MessageSendContext context) {
        int exactCount = sendQueueRepository.countWhitelistedRecipient(
                normalize(context.getChannelType()),
                context.getRecipientValue());
        if (exactCount > 0) {
            return true;
        }
        if (!isSmsChannel(context.getChannelType())) {
            return false;
        }
        String normalizedPhone = normalizePhoneNumber(context.getRecipientValue());
        if (normalizedPhone.equals(context.getRecipientValue())) {
            return false;
        }
        return sendQueueRepository.countWhitelistedRecipient(normalize(context.getChannelType()), normalizedPhone) > 0;
    }

    private boolean isSmsChannel(String channelType) {
        String normalized = normalize(channelType);
        return "SMS".equals(normalized) || "LMS".equals(normalized);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }
}
