package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HistoryChannelResponseDTO {
    // 전송 기록별 채널 목록
    // 여러 개의 채널로 전송 가능하므로 DTO에 감싸서 반환
    private Long sendHistoryId;
    private String channelName;
}
