package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateStatResponse {

    private int totalCount;
    private int aiGeneratedCount;
    private int kakaoApprovedCount;
    private int totalUseCount;
}
