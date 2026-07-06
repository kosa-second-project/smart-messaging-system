package com.example.smartmessaging.dto.request;

import com.example.smartmessaging.dto.vo.TemplateCategory;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Data
public class TemplateSaveRequest {

    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private Boolean isAiGenerated;
    @NotNull
    private TemplateCategory category;
    @NotBlank
    private String purpose;
    @NotEmpty
    private List<@NotNull @Positive Long> channelIds;
}
