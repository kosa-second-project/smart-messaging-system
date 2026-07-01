package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendTargetVO extends BaseVO {
    private Long id;
    private Long sendHistoryId;
    private Long customerId;
    private Long finalChannelId;
    private String status;
    private BigDecimal cost;
    private String userUuid;
}
