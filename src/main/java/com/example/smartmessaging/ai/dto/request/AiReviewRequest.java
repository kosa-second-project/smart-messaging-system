package com.example.smartmessaging.ai.dto.request;

import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.TemplateCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AiReviewRequest {

    // 메세지 발송/템플릿 생성
    private AiContextType contextType;
    // 광고성/정보성
    private MessageType messageType;

    private List<ChannelType> channels;

    private List<String> customerTags;
    private String title;
    private String content;
    private List<String> availableVariables;

    // 1차 서버 룰에서는 category, templateId, userId는 검증에 사용 X, 모두 선택값
    private TemplateCategory category;

    // 직접 작성한 메세지를 검사할수도 있으므로 templateId 유무 관련 예외처리 X
    private Long templateId;
    private Long userId;
}
