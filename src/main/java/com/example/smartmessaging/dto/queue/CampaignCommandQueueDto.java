package com.example.smartmessaging.dto.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCommandQueueDto {
    public static final String TYPE = "CAMPAIGN_COMMAND";

    private String type;
    private Long sendHistoryId;
    private Long userId;
    private String draftId;
    private String title;
    private String content;
    private String linkUrl;
    private Boolean advertising;
    private LocalDateTime scheduledAt;
}
