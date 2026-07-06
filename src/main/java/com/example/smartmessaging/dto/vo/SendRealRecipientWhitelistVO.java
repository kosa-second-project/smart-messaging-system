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
public class SendRealRecipientWhitelistVO extends BaseVO {
    private Long id;
    private Long customerId;
    private Long channelId;
    private String description;
    private Boolean isActive;
}
