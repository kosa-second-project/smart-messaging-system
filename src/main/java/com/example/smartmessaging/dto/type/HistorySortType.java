package com.example.smartmessaging.dto.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum HistorySortType {
    LATEST("latest"),
    OLDEST("oldest"),
    MOST_SENT("mostSent"),
    HIGHEST_SUCCESS_RATE("highestSuccessRate");

    private final String value;

    public static HistorySortType fromValueOrDefault(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElse(LATEST);
    }
}
