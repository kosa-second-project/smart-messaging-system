package com.example.smartmessaging.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSendRequestDTO {
    
    // 1. 메시지 콘텐츠 정보
    private String title;             // 메시지 제목 (LMS/이메일 등 제목 필요시)
    private String content;           // 메시지 본문 내용
    private String originalUrl;       // 메시지 내 삽입할 원본 긴 URL (선택, 단축 URL 변환용)
    private Boolean isUrlIncluded;    // 메시지 내 URL 포함 여부 플래그
    private LocalDateTime scheduledAt; // 예약 발송 일시 (즉시 발송인 경우 null)
    private String draftId;           // Redis에 보관된 수신자 Draft 임시 ID




    // 2. 발송 정책 및 목적
    private String purpose;           // 발송 목적 ('INFO' 또는 'AD' 광고성)
    private Long templateId;          // 사전에 생성된 템플릿 사용 시 (선택)

    // 3. 수신 타겟 지정 (둘 중 하나는 필수)
    private List<Long> targetTagIds;       // 발송 대상 태그 ID 목록 (대량 타겟팅 시 사용)
    private List<Long> targetCustomerIds; // 특정 수신자 ID를 직접 일괄 지정할 때 사용

    // 4. 발송 채널 우선순위 (우선순위 정렬 순서대로 수신)
    // 예: [2, 1] -> 1순위 카카오톡(channel_id=2), 실패 시 2순위 SMS(channel_id=1) 폴백
    private List<Long> routingChannelIds; 
}
