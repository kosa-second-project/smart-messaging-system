package com.example.smartmessaging.dto.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueueDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long sendTargetId;  // send_target 테이블의 PK (상태 업데이트용)
    private Long customerId;    // 고객 ID
    private String recipientNo; // 수신 번호 (또는 이메일)
    private String channelType; // "SMS", "KAKAO", "EMAIL"
    private String title;       // 메시지 제목
    private String content;     // 메시지 내용
}
