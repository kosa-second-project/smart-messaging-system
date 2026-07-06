package com.example.smartmessaging.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageTaskDto {
    private String messageId;        // 발송 고유 ID (UUID)
    private Long sendHistoryId;      // 발송 요청 ID
    private Long sendTargetId;       // 고객별 발송 대상 ID
    private Long templateId;         // 템플릿 ID
    private Long campaignId;         // 캠페인 ID
    private Long customerId;
    private Long userId;         // 발송 요청 사용자 ID
    private String phoneNumber;      // 수신자 번호 (카카오, SMS용)
    private String email;            // 수신자 이메일
    private String kakaoUserKey;     // 카카오 외부 식별자(있는 경우)
    private String title;            // 메시지 제목
    private String content;          // 메시지 본문
    private String purpose;          // 광고/정보 목적
    private String actionButtonName; // 링크 버튼명 또는 문자 링크 라벨
    private String actionUrl;        // 클릭/구매 추적용 내부 단축 URL
    private String unsubscribeUrl;   // 광고성 문자 수신거부 내부 URL
    private List<String> fallbackSequence; // 발송 우선순위 리스트 (예: ["KAKAO", "SMS"])
    private int currentStep;         // 현재 시도 단계 (0부터 시작)
}
