package com.example.smartmessaging.ai.rag.controller;

import com.example.smartmessaging.ai.rag.config.RagProperties;
import com.example.smartmessaging.ai.rag.dto.RagErrorResponseDTO;
import com.example.smartmessaging.ai.rag.dto.RagReindexResponseDTO;
import com.example.smartmessaging.ai.rag.dto.RagSearchResponseDTO;
import com.example.smartmessaging.ai.rag.dto.RagStatusResponseDTO;
import com.example.smartmessaging.ai.rag.service.RagIngestionException;
import com.example.smartmessaging.ai.rag.service.RagIngestionService;
import com.example.smartmessaging.ai.rag.service.RagSearchException;
import com.example.smartmessaging.ai.rag.service.RagSearchService;
import com.example.smartmessaging.ai.rag.service.RagStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI RAG 관리", description = "개발/관리용 RAG 적재 및 검색 테스트 API")
@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class RagApiController {

    private final RagIngestionService ragIngestionService;
    private final RagSearchService ragSearchService;
    private final RagStatusService ragStatusService;
    private final RagProperties ragProperties;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:smart-message-rag}")
    private String collectionName;

    @Operation(
            summary = "RAG seed 재색인",
            description = "개발/관리용 API입니다. classpath JSONL seed를 읽어 Qdrant collection에 적재합니다."
    )
    @PostMapping("/reindex")
    public ResponseEntity<RagReindexResponseDTO> reindex() {
        return ResponseEntity.ok(ragIngestionService.reindex());
    }

    @Operation(
            summary = "RAG 유사도 검색 테스트",
            description = "개발/관리용 API입니다. 입력 query로 Qdrant similaritySearch를 수행합니다."
    )
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query) {
        if (!StringUtils.hasText(query)) {
            return ResponseEntity.badRequest()
                    .body(RagErrorResponseDTO.failed("query must not be blank."));
        }

        RagSearchResponseDTO response = ragSearchService.search(query.trim());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "RAG 상태 확인",
            description = "개발/관리용 API입니다. 애플리케이션 메모리에 저장된 마지막 reindex 상태를 반환합니다."
    )
    @GetMapping("/status")
    public ResponseEntity<RagStatusResponseDTO> status() {
        return ResponseEntity.ok(ragStatusService.getStatus());
    }

    @ExceptionHandler(RagIngestionException.class)
    public ResponseEntity<RagReindexResponseDTO> handleIngestionException(RagIngestionException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RagReindexResponseDTO.failed(
                        ragProperties.getSeed().getPath(),
                        collectionName,
                        "RAG reindex failed. Check server logs."
                ));
    }

    @ExceptionHandler(RagSearchException.class)
    public ResponseEntity<RagErrorResponseDTO> handleSearchException(RagSearchException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RagErrorResponseDTO.failed("RAG search failed. Check server logs."));
    }
}
