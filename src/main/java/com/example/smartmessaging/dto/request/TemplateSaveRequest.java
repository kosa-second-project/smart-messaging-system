package com.example.smartmessaging.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class TemplateSaveRequest {

    private String title;
    private String content;
    private Boolean isAiGenerated;
    private String category;
    private String purpose;
    private List<Long> channelIds;
    private List<Long> tagIds;
}
