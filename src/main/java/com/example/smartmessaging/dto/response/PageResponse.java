package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.ToString;
import java.util.List;

@Getter
@ToString
public class PageResponse<T> {
    
    private List<T> list;          // 데이터 목록
    private int totalCount;        // 총 데이터 수
    private int page;              // 현재 페이지 번호
    private int size;              // 한 페이지 당 노출 수
    private int totalPages;        // 총 페이지 수

    public PageResponse(List<T> list, int totalCount, int page, int size) {
        this.list = list;
        this.totalCount = totalCount;
        this.page = page;
        this.size = size;
        this.totalPages = (int) Math.ceil((double) totalCount / size);
    }
}
