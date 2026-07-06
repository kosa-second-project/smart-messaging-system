package com.example.smartmessaging.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DevTestSendRequest {
    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 1000)
    private String content;

    @NotBlank
    @Pattern(regexp = "AD|INFO|INFORMATIONAL|ad|info|informational")
    private String purpose;

    @Size(max = 40)
    private String linkButtonName;

    @NotBlank
    @Size(max = 2048)
    @Pattern(regexp = "https?://.+", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String linkUrl;

    @Pattern(regexp = "CLICK|PURCHASE|click|purchase")
    private String linkPurpose;
}
