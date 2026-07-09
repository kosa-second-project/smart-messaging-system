package com.example.smartmessaging.ai.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private final Seed seed = new Seed();
    private final Search search = new Search();
    private final Prompt prompt = new Prompt();

    @Getter
    @Setter
    public static class Seed {
        private String path = "classpath:rag-seed/marketing-rag.jsonl";
        private boolean autoIngest = false;
    }

    @Getter
    @Setter
    public static class Search {
        private int topK = 5;
        // Swagger 검색 API 기본 topK와 분리해서, AI 프롬프트에 넣는 참고자료 수만 줄인다.
        private int promptTopK = 3;
        // RAG 지연이 AI 추천/검사 전체 지연으로 번지지 않도록 내부 참고자료 검색에만 timeout을 둔다.
        private long timeoutMs = 1500;
    }

    @Getter
    @Setter
    public static class Prompt {
        // 참고자료 본문이 길수록 LLM 입력 토큰과 응답 지연이 늘어 짧게 잘라 넣는다.
        private int maxContentLength = 300;
    }
}
