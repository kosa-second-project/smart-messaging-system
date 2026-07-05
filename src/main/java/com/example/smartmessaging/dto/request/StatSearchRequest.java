package com.example.smartmessaging.dto.request;

import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
public class StatSearchRequest {

    private static final long MAX_SEARCH_DAYS = 365;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate from;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate to;

    private String channel;

    public void validate() {
        if (from == null || to == null) {
            return;
        }

        if (from.isAfter(to)) {
            LocalDate originalFrom = from;
            from = to;
            to = originalFrom;
        }

        long selectedDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (selectedDays > MAX_SEARCH_DAYS) {
            throw new BusinessException("통계 조회 기간은 최대 365일까지 선택할 수 있습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
