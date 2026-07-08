package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.TemplateSaveRequest;
import com.example.smartmessaging.dto.request.TemplateSearchRequest;
import com.example.smartmessaging.dto.response.PageResponse;
import com.example.smartmessaging.dto.response.TemplateOptionResponse;
import com.example.smartmessaging.dto.response.TemplateResponse;
import com.example.smartmessaging.dto.response.TemplateStatResponse;

public interface TemplateService {

    PageResponse<TemplateResponse> getTemplateList(TemplateSearchRequest request);

    TemplateResponse getTemplateDetail(Long userId, Long templateId);

    TemplateOptionResponse getTemplateOptions(Long userId);

    TemplateStatResponse getTemplateStats(Long userId);

    Long createTemplate(Long userId, TemplateSaveRequest request);

    void updateTemplate(Long userId, Long templateId, TemplateSaveRequest request);

    void deleteTemplate(Long userId, Long templateId);
}
