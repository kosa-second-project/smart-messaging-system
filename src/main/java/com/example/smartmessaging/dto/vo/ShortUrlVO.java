package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortUrlVO extends BaseVO {
    private String id;
    private String originalUrl;
    private BigDecimal sendTargetId;
    private Boolean isClicked;
    private LocalDateTime clickedAt;
    private Boolean isConverted;
}
