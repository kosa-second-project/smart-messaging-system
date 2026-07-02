package com.example.smartmessaging.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 고객 목록 조회 API 응답 DTO (단건)
 * - CustomerVO의 전체 컬럼 중 화면에 필요한 필드만 노출
 * - 성별/나이/유형/수신동의 등은 tags 목록으로 통합 표현
 */
@Getter
@Setter
@Builder
public class CustomerSummaryResponse {

    private Long id;
    private String name;
    private String phone;
    private List<String> tags; // TAG 테이블에서 조회한 태그 이름 목록 (예: "남자", "30대", "카카오 동의")
    private boolean isInDraft; // 현재 임시 저장된 수신자(Draft)에 포함되어 있는지 여부
}
