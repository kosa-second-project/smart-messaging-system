package com.example.smartmessaging.ai.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProfanityFilterResponse {

    private String trackingId;
    private Status status;
    private List<DetectedWord> detected;
    // API가 반환하는 치환 결과이며, 사용자 원문을 변경하는 용도로 사용하지 않는다.
    private String filtered;
    private String elapsed;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Status {
        private Integer code;
        private String message;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DetectedWord {
        private Integer length;
        private String filteredWord;
    }
}
