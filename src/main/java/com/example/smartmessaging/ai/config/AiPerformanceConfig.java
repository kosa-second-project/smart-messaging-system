package com.example.smartmessaging.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration(proxyBeanMethods = false)
public class AiPerformanceConfig {

    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor(
            @Value("${ai.performance.executor.core-pool-size:4}") int corePoolSize,
            @Value("${ai.performance.executor.max-pool-size:8}") int maxPoolSize,
            @Value("${ai.performance.executor.queue-capacity:20}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ai-task-");
        executor.initialize();
        return executor;
    }
}
