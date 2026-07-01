package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailReasonVO extends BaseVO {
    private Long id;
    private String code;
    private String description;
    private Long sendAttemptId;
}
