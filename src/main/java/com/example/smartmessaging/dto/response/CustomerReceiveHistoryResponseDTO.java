package com.example.smartmessaging.dto.response;

import lombok.Data;

@Data
public class CustomerReceiveHistoryResponseDTO {
    
    private Long sendHistoryId;
    private String sentAt;          // 발송 시점 (yyyy-MM-dd HH:mm)
    private String templateName;    // 템플릿/메시지 제목
    private String channel;         // 발송 채널 (카카오톡, SMS, LMS 등)
    private String status;          // 최종 수신 결과 (성공, 실패)
    private String failReason;      // 실패 시 구체 사유 문구
}
