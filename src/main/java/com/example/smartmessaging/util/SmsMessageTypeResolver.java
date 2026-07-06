package com.example.smartmessaging.util;

import java.util.Locale;

public final class SmsMessageTypeResolver {

    public static final int DEFAULT_SMS_MAX_BYTES = 90;

    private SmsMessageTypeResolver() {
    }

    public static String resolve(String requestedChannel, String content) {
        return resolve(requestedChannel, content, DEFAULT_SMS_MAX_BYTES);
    }

    public static String resolve(String requestedChannel, String title, String content) {
        return resolve(requestedChannel, title, content, DEFAULT_SMS_MAX_BYTES);
    }

    public static String resolve(String requestedChannel, String content, int smsMaxBytes) {
        return resolve(requestedChannel, null, content, smsMaxBytes);
    }

    public static String resolve(String requestedChannel, String title, String content, int smsMaxBytes) {
        String normalizedChannel = normalize(requestedChannel);
        if ("LMS".equals(normalizedChannel)) {
            return "LMS";
        }
        if (title != null && !title.isBlank()) {
            return "LMS";
        }
        return getByteLength(content) > smsMaxBytes ? "LMS" : "SMS";
    }

    public static int getByteLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int byteCount = 0;
        for (int i = 0; i < text.length(); i++) {
            byteCount += text.charAt(i) <= 0x007F ? 1 : 2;
        }
        return byteCount;
    }

    public static String buildMessageText(
            String content,
            String purpose,
            String actionButtonName,
            String actionUrl,
            String unsubscribeUrl
    ) {
        StringBuilder text = new StringBuilder(content == null ? "" : content.trim());
        if (!isBlank(actionUrl)) {
            text.append("\n\n")
                    .append(isBlank(actionButtonName) ? "자세히 보기" : actionButtonName.trim())
                    .append("\n")
                    .append(actionUrl.trim());
        }
        if (isAdvertising(purpose) && !isBlank(unsubscribeUrl)) {
            text.append("\n\n수신거부를 원하시면 아래 링크를 눌러주세요.\n")
                    .append(unsubscribeUrl.trim());
        }
        return text.toString();
    }

    private static String normalize(String channelType) {
        if (channelType == null) {
            return "SMS";
        }
        String normalized = channelType.trim().toUpperCase(Locale.ROOT);
        return "LMS".equals(normalized) ? "LMS" : "SMS";
    }

    private static boolean isAdvertising(String purpose) {
        String normalized = purpose == null ? "" : purpose.trim().toUpperCase(Locale.ROOT);
        return "AD".equals(normalized);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
