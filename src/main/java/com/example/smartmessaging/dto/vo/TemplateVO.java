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
public class TemplateVO extends BaseVO {
    private BigDecimal id;
    private String title;
    private String content;
    private String kakaoTemplateCode;
    private String kakaoTemplateStatus;
    private Boolean isAiGenerated;
    private String category;
    private BigDecimal cnt;
    private String purpose;
    private BigDecimal userId;
}
