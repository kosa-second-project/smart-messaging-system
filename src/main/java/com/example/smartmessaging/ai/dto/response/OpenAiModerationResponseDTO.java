package com.example.smartmessaging.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiModerationResponseDTO(
        String id,
        String model,
        List<Result> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            Boolean flagged,
            Categories categories,
            @JsonProperty("category_scores") CategoryScores categoryScores
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Categories(
            Boolean harassment,
            @JsonProperty("harassment/threatening") Boolean harassmentThreatening,
            Boolean hate,
            @JsonProperty("hate/threatening") Boolean hateThreatening,
            Boolean illicit,
            @JsonProperty("illicit/violent") Boolean illicitViolent,
            @JsonProperty("self-harm") Boolean selfHarm,
            @JsonProperty("self-harm/intent") Boolean selfHarmIntent,
            @JsonProperty("self-harm/instructions") Boolean selfHarmInstructions,
            Boolean sexual,
            @JsonProperty("sexual/minors") Boolean sexualMinors,
            Boolean violence,
            @JsonProperty("violence/graphic") Boolean violenceGraphic
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryScores(
            Double harassment,
            @JsonProperty("harassment/threatening") Double harassmentThreatening,
            Double hate,
            @JsonProperty("hate/threatening") Double hateThreatening,
            Double illicit,
            @JsonProperty("illicit/violent") Double illicitViolent,
            @JsonProperty("self-harm") Double selfHarm,
            @JsonProperty("self-harm/intent") Double selfHarmIntent,
            @JsonProperty("self-harm/instructions") Double selfHarmInstructions,
            Double sexual,
            @JsonProperty("sexual/minors") Double sexualMinors,
            Double violence,
            @JsonProperty("violence/graphic") Double violenceGraphic
    ) {
    }
}
