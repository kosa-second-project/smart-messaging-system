package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.TemplateSaveRequest;
import com.example.smartmessaging.dto.request.TemplateSearchRequest;
import com.example.smartmessaging.dto.response.PageResponse;
import com.example.smartmessaging.dto.response.TemplateChannelResponse;
import com.example.smartmessaging.dto.response.TemplateOptionResponse;
import com.example.smartmessaging.dto.response.TemplateResponse;
import com.example.smartmessaging.dto.response.TemplateStatResponse;
import com.example.smartmessaging.dto.vo.TemplateChannelVO;
import com.example.smartmessaging.dto.vo.TemplateVO;
import com.example.smartmessaging.service.repository.TemplateMapper;
import com.example.smartmessaging.service.TemplateService;
import com.example.smartmessaging.dto.response.TemplateFilterOptionResponse;
import com.example.smartmessaging.dto.vo.TemplateCategory;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateServiceImpl implements TemplateService {

    private final TemplateMapper templateMapper;

    @Override
    public PageResponse<TemplateResponse> getTemplateList(TemplateSearchRequest request) {
        log.debug("[TemplateService] 템플릿 목록 조회 요청 - 조건: {}", request);
        List<TemplateResponse> list = templateMapper.selectTemplateList(request);
        attachChannels(list);

        int totalCount = templateMapper.selectTemplateCount(request);
        return new PageResponse<>(list, totalCount, request.getPage(), request.getSize());
    }

    @Override
    public TemplateResponse getTemplateDetail(Long userId, Long templateId) {
        log.debug("[TemplateService] 템플릿 상세 조회 요청 - TemplateID: {}", templateId);
        TemplateResponse template = templateMapper.selectTemplateDetail(userId, templateId);
        if (template != null) {
            attachChannels(List.of(template));
        }
        return template;
    }

    @Override
    public TemplateOptionResponse getTemplateOptions(Long userId) {
        TemplateOptionResponse response = new TemplateOptionResponse();
        response.setChannels(templateMapper.selectAllChannels());
        
        // TemplateCategory Enum 값들을 기반으로 정적 카테고리 필터 옵션 목록 생성
        List<TemplateFilterOptionResponse> categoryOptions = Arrays.stream(TemplateCategory.values())
                .map(cat -> new TemplateFilterOptionResponse(cat.name(), cat.getDisplayName()))
                .collect(Collectors.toList());
        response.setCategories(categoryOptions);
        
        response.setPurposes(templateMapper.selectPurposeOptions(userId));
        return response;
    }

    @Override
    public TemplateStatResponse getTemplateStats(Long userId) {
        return templateMapper.selectTemplateStats(userId);
    }

    @Override
    @Transactional
    public Long createTemplate(Long userId, TemplateSaveRequest request) {
        TemplateVO template = toTemplateVO(userId, null, request);
        templateMapper.insertTemplate(template);
        saveTemplateChannels(template.getId(), request.getChannelIds());
        return template.getId();
    }

    @Override
    @Transactional
    public void updateTemplate(Long userId, Long templateId, TemplateSaveRequest request) {
        ensureTemplateExists(userId, templateId);

        TemplateVO template = toTemplateVO(userId, templateId, request);
        int updatedCount = templateMapper.updateTemplate(template);
        if (updatedCount == 0) {
            throw templateNotFound();
        }

        // 채널 매핑은 부분 수정 대신 기존 매핑을 비활성화하고 요청값으로 다시 구성한다.
        TemplateChannelVO deleteChannels = TemplateChannelVO.builder()
                .templateId(templateId)
                .build();
        templateMapper.softDeleteTemplateChannels(deleteChannels);
        saveTemplateChannels(templateId, request.getChannelIds());
    }

    @Override
    @Transactional
    public void deleteTemplate(Long userId, Long templateId) {
        ensureTemplateExists(userId, templateId);

        TemplateVO template = TemplateVO.builder()
                .id(templateId)
                .userId(userId)
                .build();
        int deletedCount = templateMapper.softDeleteTemplate(template);
        if (deletedCount == 0) {
            throw templateNotFound();
        }

        // 템플릿 삭제 시 연결 채널도 함께 soft delete 처리해 조회 결과에서 제외한다.
        TemplateChannelVO deleteChannels = TemplateChannelVO.builder()
                .templateId(templateId)
                .build();
        templateMapper.softDeleteTemplateChannels(deleteChannels);
    }

    private void attachChannels(List<TemplateResponse> templates) {
        if (templates.isEmpty()) {
            return;
        }

        List<Long> templateIds = templates.stream()
                .map(TemplateResponse::getId)
                .collect(Collectors.toList());
        Map<Long, List<TemplateChannelResponse>> channelMap = templateMapper.selectChannelsByTemplateIds(templateIds).stream()
                .collect(Collectors.groupingBy(TemplateChannelResponse::getTemplateId));

        templates.forEach(template ->
                template.setChannels(channelMap.getOrDefault(template.getId(), List.of()))
        );
    }

    private TemplateVO toTemplateVO(Long userId, Long templateId, TemplateSaveRequest request) {
        return TemplateVO.builder()
                .id(templateId)
                .title(request.getTitle())
                .content(request.getContent())
                .isAiGenerated(Boolean.TRUE.equals(request.getIsAiGenerated()))
                .category(request.getCategory())
                .purpose(resolvePurpose(request.getPurpose()))
                .userId(userId)
                .build();
    }

    private String resolvePurpose(String purpose) {
        String value = purpose == null ? "" : purpose.trim().toLowerCase();
        if (value.equals("advertising") || value.equals("ad") || value.contains("광고")) {
            return "AD";
        }
        return "INFO";
    }

    private void saveTemplateChannels(Long templateId, List<Long> channelIds) {
        if (channelIds == null) {
            return;
        }

        for (Long channelId : channelIds) {
            TemplateChannelVO templateChannel = TemplateChannelVO.builder()
                    .templateId(templateId)
                    .channelId(channelId)
                    .build();
            templateMapper.insertTemplateChannel(templateChannel);
        }
    }

    private void ensureTemplateExists(Long userId, Long templateId) {
        // 수정/삭제 권한은 SecurityConfig에서 관리자만 통과시키고, 서비스에서는 대상 존재 여부만 확인한다.
        if (templateMapper.selectTemplateDetail(userId, templateId) == null) {
            throw templateNotFound();
        }
    }

    private BusinessException templateNotFound() {
        return new BusinessException("템플릿을 찾을 수 없습니다.", ErrorCode.TEMPLATE_NOT_FOUND);
    }
}
