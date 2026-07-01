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
public class SendAttemptVO extends BaseVO {
    private Long id;
    private Long sendTargetId;
    private Integer attemptOrder;
    private Long channelId;
    private Boolean isSucceeded;
    private String solapiMessageId;
}
