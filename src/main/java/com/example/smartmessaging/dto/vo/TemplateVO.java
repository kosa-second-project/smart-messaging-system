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
public class TemplateVO extends BaseVO {
    private Long id;
    private String title;
    private String content;
    private String kakaoTemplateCode;
    private String kakaoTemplateStatus;
    private Boolean isAiGenerated;
    private TemplateCategory category;
    private Integer cnt;
    private String purpose;
    private Long userId;
}
