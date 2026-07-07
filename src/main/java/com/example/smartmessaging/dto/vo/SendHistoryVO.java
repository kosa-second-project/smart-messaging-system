package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendHistoryVO extends BaseVO {
    private Long id;
    private Long templateId;
    private Long userId;
    private String title;
    private String content;
    private String purpose;
    private String linkButtonName;
    private String linkUrl;
    private String linkPurpose;
    private String status;
    private String solapiGroupId;
    private Integer totalTargetCount;
    private Integer successCount;
    private Integer failCount;
    private BigDecimal estimatedCost;
    private BigDecimal estimatedSaving;
    private BigDecimal actualCost;
    private String kakaoAccessTokenEnc;
    private LocalDateTime scheduledAt;
    private LocalDateTime completedAt;
}
