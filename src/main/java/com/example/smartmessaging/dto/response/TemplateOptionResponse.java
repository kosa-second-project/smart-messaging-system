package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TemplateOptionResponse {

    private List<TemplateChannelResponse> channels;
    private List<TemplateFilterOptionResponse> categories;
    private List<TemplateFilterOptionResponse> purposes;
    private List<TemplateFilterOptionResponse> tags;
}
