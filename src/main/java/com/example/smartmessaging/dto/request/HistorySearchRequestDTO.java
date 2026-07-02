package com.example.smartmessaging.dto.request;

import com.example.smartmessaging.dto.type.HistorySortType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class HistorySearchRequestDTO {
    public static final int PAGE_SIZE = 10;
    private String keyword;
    private Long channelId;
    private String status;
    private String purpose;
    private List<Long> tagIds = new ArrayList<>(); // 태그는 여러 개 선택 가능
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private String sort = "latest";
    private Integer page = 1;

    // DB 조회 전 검색 조건 정리
    public void normalize() {
        keyword = trimToNull(keyword);
        status = trimToNull(status);
        purpose = trimToNull(purpose);
        tagIds = tagIds == null ? new ArrayList<>() : tagIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        sort = HistorySortType.fromValueOrDefault(sort).getValue();
        page = page == null || page < 1 ? 1 : page;
    }

    public LocalDateTime getStartAt() {
        return startDate == null ? null : startDate.atStartOfDay();
    }

    // 종료일까지 검색 대상에 포함하도록 조회 조건을 다음날 00:00:00으로 만듦
    public LocalDateTime getEndAtExclusive() {
        return endDate == null ? null : endDate.plusDays(1).atStartOfDay();
    }

    public int getOffset() {
        return (page - 1) * PAGE_SIZE;
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    public int getTagCount() {
        return tagIds.size();
    }

    public String getKeywordLikePattern() {
        if (keyword == null) {
            return null;
        }
        String escapedKeyword = keyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escapedKeyword + "%";
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
