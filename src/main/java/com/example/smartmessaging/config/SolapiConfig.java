package com.example.smartmessaging.config;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolapiConfig {

    @Bean
    public DefaultMessageService solapiMessageService(
            @Value("${solapi.api-key:}") String apiKey,
            @Value("${solapi.api-secret:}") String apiSecret
    ) {
        return SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
    }
}
