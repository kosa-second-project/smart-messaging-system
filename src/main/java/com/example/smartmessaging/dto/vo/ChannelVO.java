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
public class ChannelVO extends BaseVO {
    private Long id;
    private String channelType;
    private BigDecimal costPerMsg;
    private Integer maxLength;
    private Boolean isActive;
}
