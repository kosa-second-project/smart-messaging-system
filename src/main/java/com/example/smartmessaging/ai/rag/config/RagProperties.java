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
    }
}
