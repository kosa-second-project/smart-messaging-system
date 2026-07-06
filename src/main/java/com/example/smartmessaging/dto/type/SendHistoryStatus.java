package com.example.smartmessaging.dto.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum SendHistoryStatus {
    QUEUED("QUEUED", "대기", "history-status--queued"),
    PREPARING("PREPARING", "전개중", "history-status--preparing"),
    SCHEDULED("SCHEDULED", "예약", "history-status--scheduled"),
    SENDING("SENDING", "전송중", "history-status--sending"),
    COMPLETED("COMPLETED", "완료", "history-status--completed"),
    SENT("SENT", "완료", "history-status--completed"),
    FAILED("FAILED", "실패", "history-status--failed");

    public static final String DEFAULT_STYLE_CLASS = "history-status--default";

    private final String value;
    private final String label;
    private final String styleClass;

    public static Optional<SendHistoryStatus> fromValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(status -> status.value.equals(normalizedValue))
                .findFirst();
    }

    public static String labelOf(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return fromValue(value)
                .map(SendHistoryStatus::getLabel)
                .orElse(value);
    }

    public static String styleClassOf(String value) {
        return fromValue(value)
                .map(SendHistoryStatus::getStyleClass)
                .orElse(DEFAULT_STYLE_CLASS);
    }
}
