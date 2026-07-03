package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateChannelResponse {

    private Long templateId;
    private Long channelId;
    private String channelType;
}
