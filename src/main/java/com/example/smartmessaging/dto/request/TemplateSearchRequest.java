package com.example.smartmessaging.dto.request;

import com.example.smartmessaging.dto.vo.TemplateCategory;
import lombok.Data;

import java.util.List;

@Data
public class TemplateSearchRequest {

    private String keyword;
    private TemplateCategory category;
    private List<TemplateCategory> categories;
    private String purpose;
    private String channelType;
    private List<String> channelTypes;
    private String sortOrder = "latest";
    private Long userId;

    private int page = 1;
    private int size = 10;

    public int getOffset() {
        return (page - 1) * size;
    }
}
