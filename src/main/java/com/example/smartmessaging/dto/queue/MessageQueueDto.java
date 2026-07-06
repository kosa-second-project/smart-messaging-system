package com.example.smartmessaging.dto.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueueDto {
    public static final String TYPE = "MESSAGE_SEND";

    private String type;
    private Long sendHistoryId;
    private Long sendTargetId;
    private Long customerId;
    private Long userId;
    private String title;
    private String content;
    private String linkUrl;
    private Boolean advertising;
    private List<Long> channelSequence;
}
