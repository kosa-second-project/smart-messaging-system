package com.example.smartmessaging.dto.request;

import com.example.smartmessaging.dto.vo.TemplateCategory;
import lombok.Data;

import java.util.List;

@Data
public class TemplateSaveRequest {

    private String title;
    private String content;
    private Boolean isAiGenerated;
    private TemplateCategory category;
    private String purpose;
    private List<Long> channelIds;
}
