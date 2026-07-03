package com.example.smartmessaging.dto.response;

import com.example.smartmessaging.dto.vo.TemplateCategory;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TemplateResponse {

    private Long id;
    private String title;
    private String content;
    private String kakaoTemplateCode;
    private String kakaoTemplateStatus;
    private Boolean isAiGenerated;
    private TemplateCategory category;
    private String categoryDisplayName;
    private Integer cnt;
    private String purpose;
    private String createdAt;
    private String updatedAt;
    private List<TemplateChannelResponse> channels;

    /**
     * 카테고리 세팅 시 한글 디스플레이 명칭을 자동으로 함께 매핑합니다.
     */
    public void setCategory(TemplateCategory category) {
        this.category = category;
        if (category != null) {
            this.categoryDisplayName = category.getDisplayName();
        } else {
            this.categoryDisplayName = null;
        }
    }
}
