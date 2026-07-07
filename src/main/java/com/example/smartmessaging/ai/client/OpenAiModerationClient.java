package com.example.smartmessaging.ai.client;

import com.example.smartmessaging.ai.config.OpenAiModerationProperties;
import com.example.smartmessaging.ai.dto.request.OpenAiModerationRequestDTO;
import com.example.smartmessaging.ai.dto.response.OpenAiModerationResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.API_ERROR;
import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.DISABLED;
import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.MISSING_API_KEY;
import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.PARSING_ERROR;
import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.TIMEOUT;

@Component
public class OpenAiModerationClient {

    private static final String MODERATION_PATH = "/v1/moderations";

    private final RestTemplate restTemplate;
    private final OpenAiModerationProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenAiModerationClient(
            RestTemplateBuilder restTemplateBuilder,
            OpenAiModerationProperties properties,
            ObjectMapper objectMapper
    ) {
        this(
                restTemplateBuilder
                        .connectTimeout(properties.getConnectTimeout())
                        .readTimeout(properties.getReadTimeout())
                        .build(),
                properties,
                objectMapper
        );
    }

    // mock RestTemplate을 주입해 실제 OpenAI API를 호출하지 않는 클라이언트 테스트에 사용한다.
    OpenAiModerationClient(RestTemplate restTemplate, OpenAiModerationProperties properties) {
        this(restTemplate, properties, new ObjectMapper());
    }

    OpenAiModerationClient(
            RestTemplate restTemplate,
            OpenAiModerationProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public OpenAiModerationResponseDTO moderate(String input) {
        // 비활성화와 key 누락은 외부 요청 전에 구분해 안전한 실패 유형으로 전달한다.
        if (!properties.isEnabled()) {
            throw new OpenAiModerationException(DISABLED);
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new OpenAiModerationException(MISSING_API_KEY);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(properties.getApiKey());

        HttpEntity<OpenAiModerationRequestDTO> request = new HttpEntity<>(
                new OpenAiModerationRequestDTO(properties.getModel(), input),
                headers
        );

        try {
            OpenAiModerationResponseDTO response = restTemplate.postForObject(
                    moderationUrl(),
                    request,
                    OpenAiModerationResponseDTO.class
            );
            if (response == null) {
                throw new OpenAiModerationException(PARSING_ERROR);
            }
            return response;
        } catch (OpenAiModerationException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new OpenAiModerationException(TIMEOUT, exception);
        } catch (RestClientResponseException exception) {
            // 원문 오류 메시지는 키나 요청 내용을 포함할 수 있어 구조화된 진단 필드만 보존한다.
            throw apiError(exception);
        } catch (RestClientException exception) {
            throw new OpenAiModerationException(PARSING_ERROR, exception);
        }
    }

    private OpenAiModerationException apiError(RestClientResponseException exception) {
        JsonNode error = parseError(exception.getResponseBodyAsString());
        String requestId = exception.getResponseHeaders() == null
                ? null
                : exception.getResponseHeaders().getFirst("x-request-id");

        return new OpenAiModerationException(
                API_ERROR,
                exception.getStatusCode().value(),
                requestId,
                textValue(error, "type"),
                textValue(error, "code"),
                exception
        );
    }

    private JsonNode parseError(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root == null ? null : root.path("error");
        } catch (JsonProcessingException exception) {
            // 오류 본문을 파싱하지 못해도 HTTP 상태와 request ID는 진단 정보로 유지한다.
            return null;
        }
    }

    private String textValue(JsonNode error, String fieldName) {
        if (error == null || error.isMissingNode() || error.isNull()) {
            return null;
        }
        JsonNode value = error.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String moderationUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + MODERATION_PATH;
    }
}
