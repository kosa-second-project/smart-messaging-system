package com.example.smartmessaging.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CustomerSearchDTO {
    
    private String name;
    private String phone;
    private String customerType;      // 일반, 신규, 휴면
    private List<String> customerTypes = new ArrayList<>();
    private String searchTag;         // 타겟/태그명 검색
    private Boolean isRejectedOnly = false; // 수신거부자 목록 탭 전용 필터
    private String sortOrder = "latest";    // 정렬 순서 (latest, name, type)
    
    // 페이징 처리 용도 (기본값 설정)
    private int page = 1;
    private int size = 10;

    public int getOffset() {
        return (page - 1) * size;
    }

    public void setSize(int size) {
        this.size = Math.min(Math.max(size, 10), 100);
    }
}
