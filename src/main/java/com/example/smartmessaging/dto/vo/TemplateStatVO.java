package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateStatVO extends BaseVO {
    private Long id;
    private Long templateId;
    private Integer rank;
    private LocalDate date;
    private Integer count;
    private Integer clickTargetCount;
    private Integer clickCount;
    private Integer conversionTargetCount;
    private Integer conversionCount;
}
