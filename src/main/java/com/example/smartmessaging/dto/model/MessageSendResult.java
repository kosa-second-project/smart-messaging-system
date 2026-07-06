package com.example.smartmessaging.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendResult {
    private boolean succeeded;
    private boolean mock;
    private String providerMessageId;
    private String failureReason;

    public static MessageSendResult success(boolean mock, String providerMessageId) {
        return MessageSendResult.builder()
                .succeeded(true)
                .mock(mock)
                .providerMessageId(providerMessageId)
                .build();
    }

    public static MessageSendResult failure(String failureReason) {
        return MessageSendResult.builder()
                .succeeded(false)
                .failureReason(failureReason)
                .build();
    }
}
