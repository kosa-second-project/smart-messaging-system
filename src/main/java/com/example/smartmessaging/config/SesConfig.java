package com.example.smartmessaging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Slf4j
@Configuration
public class SesConfig {

    @Value("${aws.ses.region:ap-northeast-2}")
    private String region;

    @Value("${aws.ses.access-key:}")
    private String accessKey;

    @Value("${aws.ses.secret-key:}")
    private String secretKey;

    @Bean
    public SesV2Client sesV2Client() {
        AwsCredentialsProvider provider = credentialsProvider();
        log.info("[SES Config] region={}, credentialProvider={}, configuredAccessKey={}, configuredSecretKeyLength={}",
                region,
                provider.getClass().getSimpleName(),
                maskAccessKey(accessKey),
                secretKey == null ? 0 : secretKey.trim().length());
        logResolvedCredentials(provider, "bean");
        return SesV2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(provider)
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSesConfigurationOnReady() {
        log.info("[SES Config Ready] region={}, configuredAccessKey={}, configuredSecretKeyLength={}",
                region,
                maskAccessKey(accessKey),
                secretKey == null ? 0 : secretKey.trim().length());
        logResolvedCredentials(credentialsProvider(), "ready");
    }

    private AwsCredentialsProvider credentialsProvider() {
        if (hasText(accessKey) && hasText(secretKey)) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.trim(), secretKey.trim()));
        }
        return DefaultCredentialsProvider.create();
    }

    private void logResolvedCredentials(AwsCredentialsProvider provider, String phase) {
        try {
            AwsCredentials credentials = provider.resolveCredentials();
            log.info("[SES Config Resolved] phase={}, resolvedAccessKey={}, resolvedSecretKeyLength={}",
                    phase,
                    maskAccessKey(credentials.accessKeyId()),
                    credentials.secretAccessKey() == null ? 0 : credentials.secretAccessKey().length());
        } catch (Exception e) {
            log.warn("[SES Config Resolved] phase={}, failedToResolveCredentials={}", phase, e.getMessage());
        }
    }

    private String maskAccessKey(String value) {
        if (!hasText(value)) {
            return "<empty>";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}