package com.example.smartmessaging.dto.request;

import com.example.smartmessaging.dto.type.HistorySortType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HistorySearchRequestDTO(
        String keyword,
        Long channelId,
        String status,
        String purpose,
        List<Long> tagIds,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        String sort,
        Integer page
) {
    public static final int PAGE_SIZE = 10;

    public HistorySearchRequestDTO {
        tagIds = tagIds == null
                ? List.of()
                : tagIds.stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public HistorySearchRequestDTO normalized() {
        List<Long> normalizedTagIds = tagIds == null
                ? List.of()
                : tagIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return new HistorySearchRequestDTO(
                trimToNull(keyword),
                channelId,
                trimToNull(status),
                trimToNull(purpose),
                normalizedTagIds,
                startDate,
                endDate,
                HistorySortType.fromValueOrDefault(sort).getValue(),
                page == null || page < 1 ? 1 : page
        );
    }

    public HistorySearchRequestDTO withPage(int page) {
        return new HistorySearchRequestDTO(
                keyword,
                channelId,
                status,
                purpose,
                tagIds,
                startDate,
                endDate,
                sort,
                page
        );
    }

    public LocalDateTime startAt() {
        return startDate == null ? null : startDate.atStartOfDay();
    }

    public LocalDateTime endAtExclusive() {
        return endDate == null ? null : endDate.plusDays(1).atStartOfDay();
    }

    public int offset() {
        return (page() - 1) * PAGE_SIZE;
    }

    public int pageSize() {
        return PAGE_SIZE;
    }

    public int tagCount() {
        return tagIds.size();
    }

    public String keywordLikePattern() {
        if (keyword == null) {
            return null;
        }
        String escapedKeyword = keyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escapedKeyword + "%";
    }

    public Integer page() {
        return page == null ? 1 : page;
    }

    public String sort() {
        return sort == null ? HistorySortType.LATEST.getValue() : sort;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }


    // MyBatis XML에서 계산 프로퍼티를 JavaBean getter 방식으로 참조할 수 있도록 제공하는 호환 메서드
    // startAt은 record component가 아니라 새로 만든 계산 메서드로, MyBatis가 startAt 프로퍼티로 볼지 애매하므로 안전하게 getStartAt을 별도로 둠
    // endAtExclusive, offset, pageSize, tagCount, keywordLikePattern도 마찬가지
    public LocalDateTime getStartAt() {
        return startAt();
    }

    public LocalDateTime getEndAtExclusive() {
        return endAtExclusive();
    }

    public int getOffset() {
        return offset();
    }

    public int getPageSize() {
        return pageSize();
    }

    public int getTagCount() {
        return tagCount();
    }

    public String getKeywordLikePattern() {
        return keywordLikePattern();
    }
}
