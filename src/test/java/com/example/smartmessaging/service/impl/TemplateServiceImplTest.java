package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.TemplateSaveRequest;
import com.example.smartmessaging.dto.response.TemplateChannelResponse;
import com.example.smartmessaging.dto.response.TemplateFilterOptionResponse;
import com.example.smartmessaging.dto.response.TemplateOptionResponse;
import com.example.smartmessaging.dto.response.TemplateResponse;
import com.example.smartmessaging.dto.vo.TemplateVO;
import com.example.smartmessaging.mapper.TemplateMapper;
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
    void 옵션_조회시_DB_태그_목록을_응답에_포함한다() {
        TemplateFilterOptionResponse tag = new TemplateFilterOptionResponse();
        tag.setValue("1");
        tag.setLabel("신규");

        when(templateMapper.selectAllChannels()).thenReturn(List.of());
        when(templateMapper.selectCategoryOptions(10L)).thenReturn(List.of());
        when(templateMapper.selectPurposeOptions(10L)).thenReturn(List.of());
        when(templateMapper.selectTagOptions()).thenReturn(List.of(tag));

        TemplateOptionResponse response = templateService.getTemplateOptions(10L);

        assertThat(response.getTags())
                .hasSize(1)
                .first()
                .extracting(TemplateFilterOptionResponse::getValue, TemplateFilterOptionResponse::getLabel)
                .containsExactly("1", "신규");
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
        request.setCategory("NOTICE");
        request.setPurpose("informational");
        request.setTagIds(List.of(1L, 2L));

        templateService.createTemplate(10L, request);

        ArgumentCaptor<TemplateVO> captor = ArgumentCaptor.forClass(TemplateVO.class);
        verify(templateMapper).insertTemplate(captor.capture());

        TemplateVO template = captor.getValue();
        assertThat(template.getKakaoTemplateCode()).isNull();
        assertThat(template.getKakaoTemplateStatus()).isNull();
        assertThat(template.getPurpose()).isEqualTo("INFO");
        assertThat(request.getTagIds()).containsExactly(1L, 2L);
    }
}
