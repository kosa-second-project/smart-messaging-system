package com.example.smartmessaging.ai.client;

import com.example.smartmessaging.ai.config.ProfanityFilterProperties;
import com.example.smartmessaging.ai.dto.request.ProfanityFilterRequest;
import com.example.smartmessaging.ai.dto.response.ProfanityFilterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ProfanityFilterClient {

    private static final String FILTER_PATH = "/api/v1/filter";
    private static final String NORMAL_MODE = "NORMAL";

    private final RestTemplate restTemplate;
    private final ProfanityFilterProperties properties;

    @Autowired
    public ProfanityFilterClient(
            RestTemplateBuilder restTemplateBuilder,
            ProfanityFilterProperties properties
    ) {
        this(
                restTemplateBuilder
                        .connectTimeout(properties.getConnectTimeout())
                        .readTimeout(properties.getReadTimeout())
                        .build(),
                properties
        );
    }

    ProfanityFilterClient(RestTemplate restTemplate, ProfanityFilterProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public Optional<ProfanityFilterResponse> filter(String text) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            log.warn("욕설 필터 API key가 없어 외부 검사를 건너뜁니다.");
            return Optional.empty();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("x-api-key", properties.getApiKey());

        HttpEntity<ProfanityFilterRequest> request = new HttpEntity<>(
                new ProfanityFilterRequest(text, NORMAL_MODE),
                headers
        );

        ProfanityFilterResponse response = restTemplate.postForObject(
                filterUrl(),
                request,
                ProfanityFilterResponse.class
        );
        return Optional.ofNullable(response);
    }

    private String filterUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + FILTER_PATH;
    }
}
