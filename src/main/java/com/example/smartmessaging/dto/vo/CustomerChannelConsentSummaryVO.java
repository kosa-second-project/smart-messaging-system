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
public class CustomerChannelConsentSummaryVO {
    private Long channelId;
    private Long totalCount;
    private Long consentedCount;
    private Long rejectedCount;
}
