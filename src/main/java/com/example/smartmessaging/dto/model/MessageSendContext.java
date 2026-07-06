package com.example.smartmessaging.dto.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageSendContext {
    private Long sendHistoryId;
    private Long sendTargetId;
    private Long customerId;
    private Long channelId;
    private String channelType;
    private String recipientValue;
    private String title;
    private String content;
    private String linkUrl;
    private boolean advertising;
}
