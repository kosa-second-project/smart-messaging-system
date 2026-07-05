package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortUrlTargetVO {
    private Long sendTargetId;
    private Long sendHistoryId;
    private Long customerId;
    private Long channelId;
    private String channelType;
    private String customerName;
    private String phone;
}
