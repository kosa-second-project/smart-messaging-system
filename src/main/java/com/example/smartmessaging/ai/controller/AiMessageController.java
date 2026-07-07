package com.example.smartmessaging.ai.controller;

import com.example.smartmessaging.ai.dto.request.AiReviewRequest;
import com.example.smartmessaging.ai.dto.response.AiReviewResponse;
import com.example.smartmessaging.ai.service.AiReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 메시지 검사", description = "서버 룰, 욕설 필터, OpenAI Moderation, Gemini 문맥 검사를 병합하는 API")
@RestController
@RequestMapping("/api/ai/messages")
@RequiredArgsConstructor
public class AiMessageController {

    private final AiReviewService aiReviewService;

    @Operation(
            summary = "AI 메시지/템플릿 문구 검사",
            description = """
                    메시지 발송 전 또는 템플릿 생성 전에 작성된 문구를 검사합니다.

                    필수값:
                    - contextType: MESSAGE_SEND 또는 TEMPLATE_CREATE
                    - messageType: AD 또는 INFO
                    - channels: 하나 이상의 발송 채널
                    - title: 검사할 제목
                    - content: 검사할 본문

                    참고:
                    - availableVariables는 현재 화면에서 실제로 허용된 변수 목록입니다.
                    - 기존 세 검사 결과를 Gemini에 전달해 문맥상 오탐 가능성을 재검토합니다.
                    - Gemini는 브랜드 톤, 과장 표현, 광고성 문맥, 민감 표현, 채널 적합성, 명확성을 추가 검사합니다.
                    - LLM 호출 실패 시에도 기존 검사 결과는 유지됩니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "통합 검사 결과",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AiReviewResponse.class),
                    examples = @ExampleObject(
                            name = "LLM 문맥 이슈가 포함된 응답",
                            value = """
                                    {
                                      "status": "WARNING",
                                      "summary": "검사 결과 주의가 필요한 항목이 있습니다.",
                                      "issues": [
                                        {
                                          "ruleId": "OVERSTATED_BENEFIT",
                                          "source": "LLM_REVIEW",
                                          "severity": "MEDIUM",
                                          "status": "WARNING",
                                          "field": "content",
                                          "message": "일부 표현이 과장된 혜택 안내처럼 보일 수 있습니다.",
                                          "targetText": "역대급 혜택",
                                          "suggestion": "혜택 조건을 구체적으로 안내해 주세요."
                                        }
                                      ],
                                      "suggestedRewrite": null,
                                      "needsHumanReview": false
                                    }
                                    """
                    )
            )
    )
    @RequestBody(
            description = "검사할 메시지/템플릿 문구 정보",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AiReviewRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "광고성 메시지 검사 요청",
                                    value = """
                                            {
                                              "contextType": "MESSAGE_SEND",
                                              "messageType": "AD",
                                              "channels": ["SMS", "KAKAO"],
                                              "customerTags": ["최근구매"],
                                              "title": "6월 여름 할인 이벤트",
                                              "content": "(광고) #{고객명}님, 6월 특별 여름 세일이 시작되었습니다. 최대 30% 할인 혜택을 확인해보세요. 무료수신거부 080-000-0000",
                                              "availableVariables": ["#{고객명}"],
                                              "category": null,
                                              "templateId": null,
                                              "userId": null
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "정상 정보성 메시지 검사 요청",
                                    value = """
                                            {
                                              "contextType": "MESSAGE_SEND",
                                              "messageType": "INFO",
                                              "channels": ["SMS"],
                                              "customerTags": [],
                                              "title": "배송 안내",
                                              "content": "#{고객명}님, 주문하신 상품이 출고되었습니다.",
                                              "availableVariables": ["#{고객명}"],
                                              "category": null,
                                              "templateId": null,
                                              "userId": null
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/review")
    public ResponseEntity<AiReviewResponse> reviewMessage(
            @org.springframework.web.bind.annotation.RequestBody AiReviewRequest request
    ) {
        return ResponseEntity.ok(aiReviewService.review(request));
    }
}
