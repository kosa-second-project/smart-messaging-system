package com.example.smartmessaging.dto.response;

import com.example.smartmessaging.dto.type.SendHistoryStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class HistoryDetailResponseDTO {
    private Long sendHistoryId;
    private String title;
    private String content;
    private LocalDateTime scheduledAt;
    private String purpose;
    private String status;
    private Integer totalTargetCount;
    private Integer successCount;
    private Integer failCount;
    private BigDecimal actualCost;
    private BigDecimal estimatedSaving;
    private BigDecimal successRate;
    private List<String> channels = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private List<HistoryAttemptFlowResponseDTO> attemptFlows = new ArrayList<>();

    public void setChannels(List<String> channels) {
        this.channels = channels == null ? new ArrayList<>() : new ArrayList<>(channels);
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

    public void setAttemptFlows(List<HistoryAttemptFlowResponseDTO> attemptFlows) {
        this.attemptFlows = attemptFlows == null ? new ArrayList<>() : new ArrayList<>(attemptFlows);
    }

    public BigDecimal getDisplaySuccessRate() {
        return successRate == null
                ? BigDecimal.ZERO.setScale(1)
                : successRate.setScale(1, RoundingMode.HALF_UP);
    }

    public String getPurposeLabel() {
        return HistoryListResponseDTO.purposeLabelOf(purpose);
    }

    public String getStatusLabel() {
        return SendHistoryStatus.labelOf(status);
    }
}
