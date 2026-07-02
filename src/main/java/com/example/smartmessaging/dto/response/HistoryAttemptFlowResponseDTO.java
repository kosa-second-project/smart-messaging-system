package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HistoryAttemptFlowResponseDTO {
    private Integer attemptOrder;
    private String channelName;
    private Integer requestCount;
    private Integer successCount;
    private Integer failCount;
}
