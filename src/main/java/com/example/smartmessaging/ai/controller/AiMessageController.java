package com.example.smartmessaging.ai.controller;

import com.example.smartmessaging.ai.dto.request.AiReviewRequest;
import com.example.smartmessaging.ai.dto.response.AiReviewResponse;
import com.example.smartmessaging.ai.service.AiReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 메시지 검사", description = "메시지/템플릿 문구를 서버 룰, 욕설 필터, OpenAI Moderation으로 검사하는 API")
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
                    - messageType: AD 또는 INFO
                    - content: 검사할 본문

                    참고:
                    - availableVariables는 현재 화면에서 실제로 허용된 변수 목록입니다.
                    - OpenAI Moderation API를 기반으로 유해 가능 표현을 추가 검사합니다.
                    """
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
                                              "content": "#{고객명}님, 6월 특별 여름 세일이 시작되었습니다. 최대 30% 할인 혜택을 확인해보세요.",
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
