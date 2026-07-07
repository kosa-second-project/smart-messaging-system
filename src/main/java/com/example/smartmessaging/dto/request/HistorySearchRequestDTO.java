package com.example.smartmessaging.dto.request;

import com.example.smartmessaging.dto.type.HistorySortType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HistorySearchRequestDTO(
        String keyword,
        Long channelId,
        List<Long> channelIds,
        String status,
        List<String> statuses,
        String purpose,
        List<String> purposes,
        List<Long> tagIds,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        String sort,
        Integer size,
        Integer page
) {
    public static final int PAGE_SIZE = 10;

    public HistorySearchRequestDTO {
        channelIds = mergeLong(channelIds, channelId);
        statuses = mergeString(statuses, status);
        purposes = mergeString(purposes, purpose);
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
                mergeLong(channelIds, channelId),
                trimToNull(status),
                mergeString(statuses, status),
                trimToNull(purpose),
                mergeString(purposes, purpose),
                normalizedTagIds,
                startDate,
                endDate,
                HistorySortType.fromValueOrDefault(sort).getValue(),
                normalizeSize(size),
                page == null || page < 1 ? 1 : page
        );
    }

    public HistorySearchRequestDTO withPage(int page) {
        return new HistorySearchRequestDTO(
                keyword,
                channelId,
                channelIds,
                status,
                statuses,
                purpose,
                purposes,
                tagIds,
                startDate,
                endDate,
                sort,
                size,
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
        return (page() - 1) * pageSize();
    }

    public int pageSize() {
        return normalizeSize(size);
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

    private static List<Long> mergeLong(List<Long> values, Long legacyValue) {
        java.util.stream.Stream<Long> stream = values == null ? java.util.stream.Stream.empty() : values.stream();
        if (legacyValue != null) {
            stream = java.util.stream.Stream.concat(stream, java.util.stream.Stream.of(legacyValue));
        }
        return stream.filter(java.util.Objects::nonNull).distinct().toList();
    }

    private static List<String> mergeString(List<String> values, String legacyValue) {
        java.util.stream.Stream<String> stream = values == null ? java.util.stream.Stream.empty() : values.stream();
        if (legacyValue != null && !legacyValue.isBlank()) {
            stream = java.util.stream.Stream.concat(stream, java.util.stream.Stream.of(legacyValue));
        }
        return stream
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public HistorySearchRequestDTO(String keyword,
                                   Long channelId,
                                   String status,
                                   String purpose,
                                   List<Long> tagIds,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   String sort,
                                   Integer page) {
        this(keyword, channelId, null, status, null, purpose, null, tagIds, startDate, endDate, sort, null, page);
    }

    private static int normalizeSize(Integer size) {
        if (size == null) {
            return PAGE_SIZE;
        }
        return Math.min(Math.max(size, 10), 100);
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

    public int getSize() {
        return pageSize();
    }

    public int getTagCount() {
        return tagCount();
    }

    public String getKeywordLikePattern() {
        return keywordLikePattern();
    }
}
