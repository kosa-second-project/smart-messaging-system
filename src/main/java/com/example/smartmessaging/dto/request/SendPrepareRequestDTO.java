package com.example.smartmessaging.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SendPrepareRequestDTO {
    @NotBlank
    private String draftId;

    private Long templateId;

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 1000)
    private String content;

    @NotBlank
    private String purpose;

    @NotEmpty
    private List<String> priorities;

    @Size(max = 40)
    private String linkButtonName;

    @Size(max = 2048)
    private String linkUrl;

    @Pattern(regexp = "CLICK|PURCHASE|click|purchase")
    private String linkPurpose;

    private LocalDateTime scheduledAt;
}
