package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageStatByDegreeVO extends BaseVO {
    private Long id;
    private Integer degree;
    private Integer sendCount;
    private Integer successCount;
    private LocalDate date;
    private BigDecimal cost;
    private Long channelId;
}
