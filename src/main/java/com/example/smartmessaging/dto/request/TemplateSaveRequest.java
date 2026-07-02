package com.example.smartmessaging.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class TemplateSaveRequest {

    private String title;
    private String content;
    private String kakaoTemplateCode;
    private String kakaoTemplateStatus;
    private Boolean isAiGenerated;
    private String category;
    private String purpose;
    private List<Long> channelIds;
}
