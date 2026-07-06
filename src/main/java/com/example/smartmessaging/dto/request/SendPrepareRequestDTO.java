package com.example.smartmessaging.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SendPrepareRequestDTO {
    @NotBlank
    private String draftId;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String purpose;
    private Boolean advertising;
    private LocalDateTime scheduledAt;
    private String linkUrl;

    @NotEmpty
    private List<Long> channelPriorityIds;
}
