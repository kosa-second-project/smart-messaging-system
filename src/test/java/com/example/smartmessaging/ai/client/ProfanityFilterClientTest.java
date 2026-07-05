package com.example.smartmessaging.ai.client;

import com.example.smartmessaging.ai.config.ProfanityFilterProperties;
import com.example.smartmessaging.ai.dto.response.ProfanityFilterResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 실제 외부 API를 호출하지 않고 MockRestServiceServer로 요청과 응답 매핑을 검증한다.
 * 실제 API key를 사용하는 연동 확인은 구현 완료 후 Postman 또는 로컬 실행으로 별도 진행한다.
 */
class ProfanityFilterClientTest {

    private ProfanityFilterProperties properties;
    private MockRestServiceServer server;
    private ProfanityFilterClient client;

    @BeforeEach
    void setUp() {
        properties = new ProfanityFilterProperties();
        properties.setBaseUrl("https://filter.test/");
        properties.setApiKey("test-api-key");

        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new ProfanityFilterClient(restTemplate, properties);
    }

    @Test
    void NORMAL_모드와_API_key로_욕설_필터를_호출한다() {
        server.expect(requestTo("https://filter.test/api/v1/filter"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(content().json("""
                        {
                          "text": "검사할 본문",
                          "mode": "NORMAL"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "status": {"code": 2000},
                          "detected": [{"length": 2, "filteredWord": "나쁜말"}],
                          "filtered": "***"
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<ProfanityFilterResponse> response = client.filter("검사할 본문");

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().getStatus().getCode()).isEqualTo(2000);
        assertThat(response.orElseThrow().getDetected().get(0).getFilteredWord()).isEqualTo("나쁜말");
        server.verify();
    }

    @Test
    void API_key가_비어_있으면_외부_API를_호출하지_않는다() {
        properties.setApiKey("  ");

        assertThat(client.filter("검사할 본문")).isEmpty();
        server.verify();
    }

    @Test
    void 응답을_파싱할_수_없으면_클라이언트_예외가_발생한다() {
        server.expect(requestTo("https://filter.test/api/v1/filter"))
                .andRespond(withSuccess("{invalid-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.filter("검사할 본문"))
                .isInstanceOf(RestClientException.class);
        server.verify();
    }

    @Test
    void Spring_컨텍스트가_운영용_생성자로_빈을_생성한다() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("restTemplateBuilder", new RestTemplateBuilder());
            context.getBeanFactory().registerSingleton(
                    "profanityFilterProperties",
                    new ProfanityFilterProperties()
            );
            context.registerBean(ProfanityFilterClient.class);

            context.refresh();

            assertThat(context.getBean(ProfanityFilterClient.class)).isNotNull();
        }
    }
}
