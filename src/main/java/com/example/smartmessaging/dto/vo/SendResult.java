package com.example.smartmessaging.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendResult {
    private boolean success;
    private String channel;
    private String errorCode;
    private String errorMessage;

    public static SendResult success(String channel) {
        return SendResult.builder()
                .success(true)
                .channel(channel)
                .build();
    }

    public static SendResult fail(String channel, String errorCode, String errorMessage) {
        return SendResult.builder()
                .success(false)
                .channel(channel)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
