package com.example.smartmessaging.dto.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCommandQueueDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long sendHistoryId;
    private String draftId;
    private Long userId;
    private String title;
    private String content;
    private String purpose;
    private String linkButtonName;
    private String linkUrl;
    private String linkPurpose;
    private Long templateId;
    private List<Long> routingChannelIds;
    private String kakaoAccessToken;
}
