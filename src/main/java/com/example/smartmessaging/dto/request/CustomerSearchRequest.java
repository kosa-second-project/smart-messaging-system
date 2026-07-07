package com.example.smartmessaging.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 고객 목록 조회 API 요청 파라미터 DTO
 * - keyword: 고객명 또는 태그명 검색어
 * - tagIds: 필터할 태그 ID 목록 (성별/나이/유형/수신동의 모두 태그 기반)
 * - matchType: 태그 필터 방식 ("ANY" = 하나라도 포함, "ALL" = 모두 포함) 기본값 ANY
 * - page, size: 페이징 파라미터
 */
@Getter
@Setter
@NoArgsConstructor
public class CustomerSearchRequest {

    private String keyword;
    private List<Long> tagIds;
    private List<Long> customerIds; // 직접 선택한 고객 ID 목록 필터링용
    private String draftId; // 임시 저장소(Redis) 식별자
    private String activeTab; // "filtered" 또는 "selected"

    // 매퍼에서 하드코딩을 제거하기 위해 애플리케이션 계층에서 주입하는 상수 값 (기본값 설정)
    private List<Long> consentTagIds = List.of();

    /** "ANY"(기본) = OR 조건, "ALL" = AND 조건 */
    private String matchType = "ANY";

    private Long cursorId;

    private int page = 1;
    private int size = 20;

    public void setPage(int page) {
        this.page = Math.max(page, 1);
    }

    public void setSize(int size) {
        this.size = Math.min(Math.max(size, 1), 100);
    }

    /**
     * MyBatis OFFSET 페이징 계산용
     * Oracle: OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY
     */
    public int getOffset() {
        return (page - 1) * size;
    }

    /**
     * MyBatis HAVING 절에서 AND 조건 사용 시 필요한 태그 개수
     * HAVING COUNT(DISTINCT tag_id) = #{tagCount}
     */
    public int getTagCount() {
        return (tagIds == null) ? 0 : tagIds.size();
    }
}
