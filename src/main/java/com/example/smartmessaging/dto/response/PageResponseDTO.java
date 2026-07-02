package com.example.smartmessaging.dto.response;

import lombok.Getter;
import java.util.List;

@Getter
public class PageResponseDTO<T> {
    // 현재 페이지 목록 + 페이지네이션 화면을 그리는 데 필요한 계산값들을 담는 공통 응답 DTO
    private final List<T> content;
    private final int currentPage;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;
    private final int startPage;
    private final int endPage;
    private final long startElement;
    private final long endElement;
    private final boolean hasPrevious;
    private final boolean hasNext;

    private PageResponseDTO(List<T> content, int currentPage, int pageSize, long totalElements) {
        this.content = content;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        this.startPage = totalPages == 0 ? 0 : ((currentPage - 1) / 5) * 5 + 1;
        this.endPage = totalPages == 0 ? 0 : Math.min(startPage + 4, totalPages);
        this.startElement = totalElements == 0 ? 0 : (long) (currentPage - 1) * pageSize + 1;
        this.endElement = Math.min((long) currentPage * pageSize, totalElements);
        this.hasPrevious = currentPage > 1;
        this.hasNext = currentPage < totalPages;
    }

    public static <T> PageResponseDTO<T> of(List<T> content, int currentPage, int pageSize, long totalElements) {
        return new PageResponseDTO<>(content, currentPage, pageSize, totalElements);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }
}
