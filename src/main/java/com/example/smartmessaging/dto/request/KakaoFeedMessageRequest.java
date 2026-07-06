package com.example.smartmessaging.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KakaoFeedMessageRequest {
    private String title;
    private String description;
    private String linkButtonName;
    private String linkUrl;
    private List<String> targetUuids;
}
