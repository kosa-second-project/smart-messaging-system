package com.example.smartmessaging.dto.request;

import lombok.Data;

@Data
public class TemplateSearchRequest {

    private String keyword;
    private String category;
    private String purpose;
    private String channelType;
    private String sortOrder = "latest";
    private Long userId;

    private int page = 1;
    private int size = 10;

    public int getOffset() {
        return (page - 1) * size;
    }
}
