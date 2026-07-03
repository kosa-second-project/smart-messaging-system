package com.example.smartmessaging.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 고객 목록 조회 API 페이징 래퍼 응답 DTO
 */
@Getter
@Builder
public class PagedCustomerResponse {

    private List<CustomerSummaryResponse> content;
    private int totalCount; // 여전히 화면에 띄워줄 용도로 유지할 수 있음
    private int page; // 프론트엔드가 page 숫자를 유지하고 싶다면
    private int size;
    private int totalPages;
    private Long nextCursorId;
    private boolean hasNext;

    /**
     * Redis에서 가져온 실제 totalCount로 재조정된 응답 생성
     * selected 탭 페이징 시 totalCount와 totalPages를 Redis 기준으로 덮어씌움
     */
    public PagedCustomerResponse withTotalCount(long redisTotalCount) {
        int total = (int) redisTotalCount;
        int pages = (this.size > 0) ? (int) Math.ceil((double) total / this.size) : 0;
        return PagedCustomerResponse.builder()
                .content(this.content)
                .totalCount(total)
                .page(this.page)
                .size(this.size)
                .totalPages(pages)
                .nextCursorId(this.nextCursorId)
                .hasNext(this.hasNext)
                .build();
    }
}
