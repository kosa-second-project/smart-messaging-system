package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.TemplateSaveRequest;
import com.example.smartmessaging.dto.request.TemplateSearchRequest;
import com.example.smartmessaging.dto.response.PageResponse;
import com.example.smartmessaging.dto.response.TemplateOptionResponse;
import com.example.smartmessaging.dto.response.TemplateResponse;
import com.example.smartmessaging.dto.response.TemplateStatResponse;
import com.example.smartmessaging.security.CustomUserDetails;
import com.example.smartmessaging.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateApiController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<PageResponse<TemplateResponse>> getTemplates(
            @ModelAttribute TemplateSearchRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("[TemplateApiController] 템플릿 목록 조회 API 호출 - Parameter: {}", request);
        return ResponseEntity.ok(templateService.getTemplateList(request));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateResponse> getTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(templateService.getTemplateDetail(userDetails.getUserId(), templateId));
    }

    @GetMapping("/options")
    public ResponseEntity<TemplateOptionResponse> getTemplateOptions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(templateService.getTemplateOptions(userDetails.getUserId()));
    }

    @GetMapping("/stats")
    public ResponseEntity<TemplateStatResponse> getTemplateStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(templateService.getTemplateStats(userDetails.getUserId()));
    }

    @PostMapping
    public ResponseEntity<Long> createTemplate(
            @Valid @RequestBody TemplateSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long templateId = templateService.createTemplate(userDetails.getUserId(), request);
        return ResponseEntity.created(URI.create("/api/templates/" + templateId)).body(templateId);
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<Void> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody TemplateSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        templateService.updateTemplate(userDetails.getUserId(), templateId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        templateService.deleteTemplate(userDetails.getUserId(), templateId);
        return ResponseEntity.noContent().build();
    }
}
