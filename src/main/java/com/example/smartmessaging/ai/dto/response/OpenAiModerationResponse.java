package com.example.smartmessaging.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiModerationResponse {

    private String id;
    private String model;
    private List<Result> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private Boolean flagged;
        private Categories categories;

        @JsonProperty("category_scores")
        private CategoryScores categoryScores;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Categories {
        private Boolean harassment;

        @JsonProperty("harassment/threatening")
        private Boolean harassmentThreatening;

        private Boolean hate;

        @JsonProperty("hate/threatening")
        private Boolean hateThreatening;

        private Boolean illicit;

        @JsonProperty("illicit/violent")
        private Boolean illicitViolent;

        @JsonProperty("self-harm")
        private Boolean selfHarm;

        @JsonProperty("self-harm/intent")
        private Boolean selfHarmIntent;

        @JsonProperty("self-harm/instructions")
        private Boolean selfHarmInstructions;

        private Boolean sexual;

        @JsonProperty("sexual/minors")
        private Boolean sexualMinors;

        private Boolean violence;

        @JsonProperty("violence/graphic")
        private Boolean violenceGraphic;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CategoryScores {
        private Double harassment;

        @JsonProperty("harassment/threatening")
        private Double harassmentThreatening;

        private Double hate;

        @JsonProperty("hate/threatening")
        private Double hateThreatening;

        private Double illicit;

        @JsonProperty("illicit/violent")
        private Double illicitViolent;

        @JsonProperty("self-harm")
        private Double selfHarm;

        @JsonProperty("self-harm/intent")
        private Double selfHarmIntent;

        @JsonProperty("self-harm/instructions")
        private Double selfHarmInstructions;

        private Double sexual;

        @JsonProperty("sexual/minors")
        private Double sexualMinors;

        private Double violence;

        @JsonProperty("violence/graphic")
        private Double violenceGraphic;
    }
}
