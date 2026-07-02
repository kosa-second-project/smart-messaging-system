package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@NoArgsConstructor
public class HistoryListResponseDTO {
    private Long id;
    private LocalDateTime scheduledAt;
    private String title;
    private String purpose;
    private Integer totalTargetCount;
    private Integer successCount;
    private Integer failCount;
    private BigDecimal successRate;
    private BigDecimal estimatedSaving;
    private String status;
    private List<String> channels = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private List<String> displayTags = new ArrayList<>();
    private int hiddenTagCount;

    // 앞 3개만 보여주고 나머지는 숨김처리, 개수로 보여줌
    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.displayTags = this.tags.stream().limit(3).toList();
        this.hiddenTagCount = Math.max(this.tags.size() - this.displayTags.size(), 0);
    }

    // 성공률은 소수점 첫번째 자리까지 반올림해서 표시
    public BigDecimal getDisplaySuccessRate() {
        return successRate == null
                ? BigDecimal.ZERO.setScale(1)
                : successRate.setScale(1, RoundingMode.HALF_UP);
    }

    public String getPurposeLabel() {
        if (purpose == null || purpose.isBlank()) {
            return "-";
        }
        return switch (purpose.toUpperCase(Locale.ROOT)) {
            case "AD" -> "광고성";
            case "INFO" -> "정보성";
            default -> purpose;
        };
    }

    public String getStatusLabel() {
        return statusLabelOf(status);
    }

    // 상태값에 맞는 CSS 클래스명을 반환하는 메서드
    public String getStatusStyleClass() {
        if (status == null || status.isBlank()) {
            return "history-status--default";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "SCHEDULED" -> "history-status--scheduled";
            case "SENDING" -> "history-status--sending";
            case "SENT" -> "history-status--completed";
            case "FAILED" -> "history-status--failed";
            default -> "history-status--default";
        };
    }

    // 상태값을 화면에 보이는 글자로 바꿔주는 메서드
    public static String statusLabelOf(String status) {
        if (status == null || status.isBlank()) {
            return "-";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "SCHEDULED" -> "예약";
            case "SENDING" -> "전송중";
            case "SENT" -> "완료";
            case "FAILED" -> "실패";
            default -> status;
        };
    }
}
