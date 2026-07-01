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
public class TryHistoryVO extends BaseVO {
    private Long id;
    private Long sendHistoryId;
    private Integer degree;
    private Integer successCount;
    private Integer failCount;
}
