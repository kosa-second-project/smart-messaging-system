package com.example.smartmessaging.dto.vo;

import lombok.Getter;

@Getter
public enum TemplateCategory {
    BENEFIT("혜택"),
    EVENT("이벤트"),
    NOTICE("공지"),
    CRM("고객관리");

    private final String displayName;

    TemplateCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 문자열 카테고리명을 안전하게 Enum으로 파싱합니다.
     */
    public static TemplateCategory fromString(String categoryStr) {
        if (categoryStr == null || categoryStr.isBlank()) {
            return null;
        }
        try {
            return TemplateCategory.valueOf(categoryStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            // 맞지 않는 문자열이 들어올 경우의 기본 처리
            return null;
        }
    }
}
