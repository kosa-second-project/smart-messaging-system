package com.example.smartmessaging.dto.response;

import java.util.List;

public record PageResponseDTO<T>(
        List<T> content,
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages,
        int startPage,
        int endPage,
        long startElement,
        long endElement,
        boolean hasPrevious,
        boolean hasNext
) {
    public static <T> PageResponseDTO<T> of(List<T> content, int currentPage, int pageSize, long totalElements) {
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        int startPage = totalPages == 0 ? 0 : ((currentPage - 1) / 5) * 5 + 1;
        int endPage = totalPages == 0 ? 0 : Math.min(startPage + 4, totalPages);
        long startElement = totalElements == 0 ? 0 : (long) (currentPage - 1) * pageSize + 1;
        long endElement = Math.min((long) currentPage * pageSize, totalElements);
        return new PageResponseDTO<>(
                content,
                currentPage,
                pageSize,
                totalElements,
                totalPages,
                startPage,
                endPage,
                startElement,
                endElement,
                currentPage > 1,
                currentPage < totalPages
        );
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }
}
