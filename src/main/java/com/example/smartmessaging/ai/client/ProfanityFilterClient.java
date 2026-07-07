package com.example.smartmessaging.ai.client;

import com.example.smartmessaging.ai.config.ProfanityFilterProperties;
import com.example.smartmessaging.ai.dto.request.ProfanityFilterRequestDTO;
import com.example.smartmessaging.ai.dto.response.ProfanityFilterResponseDTO;
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

    // 테스트용 생성자와 구분할 수 있도록 Spring이 사용할 생성자 명시
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

    // 실제 외부 API를 호출하지 않는 클라이언트 테스트에서 mock RestTemplate을 주입한다.
    ProfanityFilterClient(RestTemplate restTemplate, ProfanityFilterProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public Optional<ProfanityFilterResponseDTO> filter(String text) {
        // API key 누락은 사용자 문구의 문제가 아니므로 외부 호출 없이 검사 결과를 비운다.
        if (!StringUtils.hasText(properties.getApiKey())) {
            log.warn("욕설 필터 API key가 없어 외부 검사를 건너뜁니다.");
            return Optional.empty();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("x-api-key", properties.getApiKey());

        HttpEntity<ProfanityFilterRequestDTO> request = new HttpEntity<>(
                // FILTER 모드는 원문을 치환하므로 검사 전용 NORMAL 모드만 사용한다.
                new ProfanityFilterRequestDTO(text, NORMAL_MODE),
                headers
        );

        ProfanityFilterResponseDTO response = restTemplate.postForObject(
                filterUrl(),
                request,
                ProfanityFilterResponseDTO.class
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
