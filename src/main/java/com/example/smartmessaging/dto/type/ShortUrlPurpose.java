package com.example.smartmessaging.dto.type;

import java.util.Locale;

public enum ShortUrlPurpose {
    CLICK,
    PURCHASE,
    UNSUBSCRIBE;

    public static ShortUrlPurpose from(String value) {
        if (value == null || value.isBlank()) {
            return CLICK;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (ShortUrlPurpose purpose : values()) {
            if (purpose.name().equals(normalized)) {
                return purpose;
            }
        }
        return CLICK;
    }

    public boolean countsAsClickRate() {
        return this == CLICK || this == PURCHASE;
    }
}
