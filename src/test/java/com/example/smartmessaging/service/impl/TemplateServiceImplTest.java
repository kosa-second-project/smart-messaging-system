package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.TemplateSaveRequest;
import com.example.smartmessaging.dto.response.TemplateChannelResponse;
import com.example.smartmessaging.dto.response.TemplateOptionResponse;
import com.example.smartmessaging.dto.response.TemplateResponse;
import com.example.smartmessaging.dto.vo.TemplateCategory;
import com.example.smartmessaging.dto.vo.TemplateChannelVO;
import com.example.smartmessaging.dto.vo.TemplateVO;
import com.example.smartmessaging.service.repository.TemplateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateServiceImpl templateService;

    @Test
    void 옵션_조회시_채널_고정_카테고리_광고여부를_응답한다() {
        when(templateMapper.selectAllChannels()).thenReturn(List.of());
        when(templateMapper.selectPurposeOptions(10L)).thenReturn(List.of());

        TemplateOptionResponse response = templateService.getTemplateOptions(10L);

        assertThat(response.getChannels()).isEmpty();
        assertThat(response.getCategories())
                .extracting("value", "label")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("BENEFIT", "혜택"),
                        org.assertj.core.groups.Tuple.tuple("EVENT", "이벤트"),
                        org.assertj.core.groups.Tuple.tuple("NOTICE", "공지"),
                        org.assertj.core.groups.Tuple.tuple("CRM", "고객관리")
                );
        assertThat(response.getPurposes()).isEmpty();
    }

    @Test
    void 상세_조회시_DB_템플릿에_연결된_채널을_붙인다() {
        TemplateResponse template = new TemplateResponse();
        template.setId(7L);
        template.setTitle("DB 템플릿");
        template.setContent("DB 메시지 내용");

        TemplateChannelResponse channel = new TemplateChannelResponse();
        channel.setTemplateId(7L);
        channel.setChannelId(2L);
        channel.setChannelType("SMS");

        when(templateMapper.selectTemplateDetail(10L, 7L)).thenReturn(template);
        when(templateMapper.selectChannelsByTemplateIds(List.of(7L))).thenReturn(List.of(channel));

        TemplateResponse response = templateService.getTemplateDetail(10L, 7L);

        assertThat(response.getTitle()).isEqualTo("DB 템플릿");
        assertThat(response.getContent()).isEqualTo("DB 메시지 내용");
        assertThat(response.getChannels())
                .hasSize(1)
                .first()
                .extracting(TemplateChannelResponse::getChannelType)
                .isEqualTo("SMS");
    }

    @Test
    void 생성시_카카오_값은_무시하고_DB저장용_목적값만_정규화한다() {
        TemplateSaveRequest request = new TemplateSaveRequest();
        request.setTitle("신규 템플릿");
        request.setContent("메시지 내용");
        request.setCategory(TemplateCategory.NOTICE);
        request.setPurpose("informational");

        templateService.createTemplate(10L, request);

        ArgumentCaptor<TemplateVO> captor = ArgumentCaptor.forClass(TemplateVO.class);
        verify(templateMapper).insertTemplate(captor.capture());

        TemplateVO template = captor.getValue();
        assertThat(template.getKakaoTemplateCode()).isNull();
        assertThat(template.getKakaoTemplateStatus()).isNull();
        assertThat(template.getPurpose()).isEqualTo("INFO");
    }

    @Test
    void updateTemplate_replacesTemplateAndChannels() {
        TemplateResponse existing = new TemplateResponse();
        existing.setId(7L);
        when(templateMapper.selectTemplateDetail(10L, 7L)).thenReturn(existing);
        when(templateMapper.updateTemplate(org.mockito.ArgumentMatchers.any(TemplateVO.class))).thenReturn(1);

        TemplateSaveRequest request = new TemplateSaveRequest();
        request.setTitle("Updated");
        request.setContent("Updated content");
        request.setCategory(TemplateCategory.EVENT);
        request.setPurpose("AD");
        request.setIsAiGenerated(true);
        request.setChannelIds(List.of(1L, 2L));

        templateService.updateTemplate(10L, 7L, request);

        ArgumentCaptor<TemplateVO> templateCaptor = ArgumentCaptor.forClass(TemplateVO.class);
        verify(templateMapper).updateTemplate(templateCaptor.capture());
        assertThat(templateCaptor.getValue().getId()).isEqualTo(7L);
        assertThat(templateCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(templateCaptor.getValue().getTitle()).isEqualTo("Updated");

        ArgumentCaptor<TemplateChannelVO> channelCaptor = ArgumentCaptor.forClass(TemplateChannelVO.class);
        verify(templateMapper).softDeleteTemplateChannels(channelCaptor.capture());
        assertThat(channelCaptor.getValue().getTemplateId()).isEqualTo(7L);
        verify(templateMapper, org.mockito.Mockito.times(2))
                .insertTemplateChannel(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteTemplate_softDeletesTemplateAndChannels() {
        TemplateResponse existing = new TemplateResponse();
        existing.setId(7L);
        when(templateMapper.selectTemplateDetail(10L, 7L)).thenReturn(existing);
        when(templateMapper.softDeleteTemplate(org.mockito.ArgumentMatchers.any(TemplateVO.class))).thenReturn(1);

        templateService.deleteTemplate(10L, 7L);

        ArgumentCaptor<TemplateVO> templateCaptor = ArgumentCaptor.forClass(TemplateVO.class);
        verify(templateMapper).softDeleteTemplate(templateCaptor.capture());
        assertThat(templateCaptor.getValue().getId()).isEqualTo(7L);
        assertThat(templateCaptor.getValue().getUserId()).isEqualTo(10L);
        verify(templateMapper).softDeleteTemplateChannels(org.mockito.ArgumentMatchers.any());
    }
}
