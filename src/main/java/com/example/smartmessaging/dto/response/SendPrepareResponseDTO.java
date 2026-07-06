package com.example.smartmessaging.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendPrepareResponseDTO {
    private Long sendHistoryId;
    private String status;
    private Boolean queued;
    private LocalDateTime scheduledAt;
    private String message;
}
