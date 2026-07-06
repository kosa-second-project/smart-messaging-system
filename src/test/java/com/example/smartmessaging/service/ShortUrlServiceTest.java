package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ShortUrlTargetVO;
import com.example.smartmessaging.dto.vo.ShortUrlVO;
import com.example.smartmessaging.mapper.ShortUrlMapper;
import com.example.smartmessaging.mapper.UnsubscribeMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortUrlServiceTest {

    private final ShortUrlMapper shortUrlMapper = mock(ShortUrlMapper.class);
    private final UnsubscribeMapper unsubscribeMapper = mock(UnsubscribeMapper.class);
    private final ShortUrlService shortUrlService = new ShortUrlService(shortUrlMapper, unsubscribeMapper);

    @Test
    void 추적링크는_kosa_짧은도메인과_8자리_코드를_사용한다() {
        ReflectionTestUtils.setField(shortUrlService, "appBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(shortUrlService, "shortUrlBaseUrl", "https://kosa.kr");
        when(shortUrlMapper.insertShortUrl(org.mockito.ArgumentMatchers.any(ShortUrlVO.class))).thenReturn(1);
        ShortUrlTargetVO target = new ShortUrlTargetVO();
        target.setChannelId(3L);
        when(shortUrlMapper.findTargetBySendTargetId(10L)).thenReturn(target);

        String url = shortUrlService.createTrackedUrl(10L, "https://example.com/coupon", ShortUrlPurpose.CLICK);

        assertThat(url).matches("https://kosa\\.kr/r/[0-9A-Za-z]{8}");

        ArgumentCaptor<ShortUrlVO> rowCaptor = ArgumentCaptor.forClass(ShortUrlVO.class);
        org.mockito.Mockito.verify(shortUrlMapper).insertShortUrl(rowCaptor.capture());
        assertThat(rowCaptor.getValue().getId()).hasSize(8);
        assertThat(rowCaptor.getValue().getOriginalUrl()).isEqualTo("https://example.com/coupon");
        assertThat(rowCaptor.getValue().getSendTargetId()).isEqualTo(10L);
        assertThat(rowCaptor.getValue().getPurpose()).isEqualTo("CLICK");
        verify(shortUrlMapper).incrementChannelClickTargetCount(3L, 1L);
    }

    @Test
    void 클릭시_send_target의_회원UUID와_채널ID를_원본URL에_붙이고_첫클릭_통계를_반영한다() {
        ShortUrlTargetVO target = new ShortUrlTargetVO();
        target.setOriginalUrl("https://example.com/coupon?utm=summer");
        target.setPurpose("CLICK");
        target.setSendTargetId(10L);
        target.setUserUuid("user-uuid-1");
        target.setChannelId(7L);
        when(shortUrlMapper.findClickTargetById("Ab3dE5gH")).thenReturn(target);
        when(shortUrlMapper.markFirstClicked("Ab3dE5gH")).thenReturn(1);

        String redirectUrl = shortUrlService.markClickAndResolveRedirect("Ab3dE5gH");

        assertThat(redirectUrl)
                .isEqualTo("https://example.com/coupon?utm=summer&userUuid=user-uuid-1&channelId=7");
        verify(shortUrlMapper).incrementChannelClickCount(7L, 1L);
        verify(shortUrlMapper).incrementHourlyClickCount(eq(7L), anyInt(), eq(1L));
    }
}
