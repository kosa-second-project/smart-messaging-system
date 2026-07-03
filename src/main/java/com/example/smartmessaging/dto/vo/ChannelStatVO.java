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
public class ChannelStatVO extends BaseVO {
    private Long id;
    private Integer clickTargetCount;
    private Integer clickCount;
    private Integer conversionTargetCount;
    private Integer conversionCount;
    private Integer consentTargetCount;
    private Integer consentCount;
    private Long channelId;
}
