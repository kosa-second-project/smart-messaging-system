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
import com.example.smartmessaging.mapper.TemplateMapper;
import com.example.smartmessaging.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        response.setCategories(templateMapper.selectCategoryOptions(userId));
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
                .kakaoTemplateCode(request.getKakaoTemplateCode())
                .kakaoTemplateStatus(request.getKakaoTemplateStatus() == null ? "PENDING" : request.getKakaoTemplateStatus())
                .isAiGenerated(Boolean.TRUE.equals(request.getIsAiGenerated()))
                .category(request.getCategory())
                .purpose(request.getPurpose())
                .userId(userId)
                .build();
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
}
