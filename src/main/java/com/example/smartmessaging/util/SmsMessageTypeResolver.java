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

    private static String normalize(String channelType) {
        if (channelType == null) {
            return "SMS";
        }
        String normalized = channelType.trim().toUpperCase(Locale.ROOT);
        return "LMS".equals(normalized) ? "LMS" : "SMS";
    }
}
