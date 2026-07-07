package com.example.smartmessaging.ai.dto.request;

import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.TemplateCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AiSuggestionRequest {

    // 추천 결과가 사용될 화면 문맥(메시지 발송/템플릿 생성)
    @NotNull
    private AiContextType contextType;

    // 광고성 여부. 생성 프롬프트와 본문 서버 룰 검증에 함께 사용한다.
    @NotNull
    private MessageType messageType;

    @NotEmpty
    private List<@NotNull ChannelType> channels;

    private List<@NotBlank String> customerTags = List.of();

    private String direction;

    // 템플릿 생성 문맥에서만 프롬프트에 반영한다.
    private TemplateCategory category;

    // 미입력 시 프로젝트 기본 변수 3종을 사용한다.
    @Valid
    private List<@NotBlank String> availableVariables;
}
