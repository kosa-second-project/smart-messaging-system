package com.example.smartmessaging.ai.rag.service;

import com.example.smartmessaging.ai.rag.config.RagProperties;
import com.example.smartmessaging.ai.rag.dto.RagStatusResponseDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RagStatusServiceTest {

    @Test
    void statusTransitionsFromNotIndexedToReadyAndFailed() {
        RagProperties properties = new RagProperties();
        RagStatusService service = new RagStatusService(properties, "smart-message-rag");

        RagStatusResponseDTO initial = service.getStatus();
        assertThat(initial.status()).isEqualTo("NOT_INDEXED");
        assertThat(initial.lastIndexedCount()).isZero();
        assertThat(initial.lastIndexedAt()).isNull();

        LocalDateTime indexedAt = LocalDateTime.of(2026, 7, 7, 23, 10);
        service.markReady(193, indexedAt);

        RagStatusResponseDTO ready = service.getStatus();
        assertThat(ready.status()).isEqualTo("READY");
        assertThat(ready.lastIndexedCount()).isEqualTo(193);
        assertThat(ready.lastIndexedAt()).isEqualTo(indexedAt);

        service.markFailed();

        RagStatusResponseDTO failed = service.getStatus();
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.lastIndexedCount()).isEqualTo(193);
        assertThat(failed.lastIndexedAt()).isEqualTo(indexedAt);
    }
}
