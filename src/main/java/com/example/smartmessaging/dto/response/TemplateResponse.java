package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TemplateResponse {

    private Long id;
    private String title;
    private String content;
    private String kakaoTemplateCode;
    private String kakaoTemplateStatus;
    private Boolean isAiGenerated;
    private String category;
    private Integer cnt;
    private String purpose;
    private String createdAt;
    private String updatedAt;
    private List<TemplateChannelResponse> channels;
}
