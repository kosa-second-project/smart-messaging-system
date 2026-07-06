package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortUrlTargetVO {
    private String shortUrlId;
    private String originalUrl;
    private String purpose;
    private Long sendTargetId;
    private Long sendHistoryId;
    private Long customerId;
    private String userUuid;
    private Long channelId;
    private String channelType;
    private String customerName;
    private String phone;
}
