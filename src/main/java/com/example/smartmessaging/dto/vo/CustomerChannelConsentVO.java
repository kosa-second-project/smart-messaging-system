package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerChannelConsentVO extends BaseVO {
    private Long id;
    private Boolean isConsented;
    private Long customerId;
    private Long channelId;
}
