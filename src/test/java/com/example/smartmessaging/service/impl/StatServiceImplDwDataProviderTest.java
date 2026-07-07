package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.StatPageResponse;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.MessageStatByDegreeVO;
import com.example.smartmessaging.dto.vo.MessageStatVO;
import com.example.smartmessaging.dw.DwStatDataProvider;
import com.example.smartmessaging.service.repository.StatMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatServiceImplDwDataProviderTest {

    private final StatMapper statMapper = mock(StatMapper.class);
    private final DwStatDataProvider dwStatDataProvider = mock(DwStatDataProvider.class);
    private final StatServiceImpl service = new StatServiceImpl(statMapper, dwStatDataProvider);

    @Test
    void deliveryStats_useDwDataWhenAvailable() {
        StatSearchRequest request = request();
        when(dwStatDataProvider.selectMessageStats(request)).thenReturn(Optional.of(List.of(
                MessageStatVO.builder()
                        .date(LocalDate.of(2026, 7, 1))
                        .totalSendCount(12)
                        .totalSuccessCount(9)
                        .billingCost(BigDecimal.valueOf(1200))
                        .maxCost(BigDecimal.valueOf(1800))
                        .build()
        )));
        when(dwStatDataProvider.selectDeliveryMessageStatByDegrees(request)).thenReturn(Optional.of(List.of(
                MessageStatByDegreeVO.builder()
                        .date(LocalDate.of(2026, 7, 1))
                        .degree(1)
                        .channelId(1L)
                        .sendCount(12)
                        .successCount(9)
                        .cost(BigDecimal.valueOf(1200))
                        .build()
        )));
        when(dwStatDataProvider.selectActiveChannels()).thenReturn(Optional.of(List.of(channel(1L, "SMS"))));

        StatPageResponse response = service.getDeliveryStats(request);

        assertThat(response.getCards().get(0).getValue()).isEqualTo("12");
        assertThat(response.getCharts().get(0).getDatasets().get(0).getLabel()).isEqualTo("SMS");
        verify(statMapper, never()).selectMessageStats(request);
        verify(statMapper, never()).selectDeliveryMessageStatByDegrees(request);
    }

    @Test
    void deliveryStats_fallsBackToMapperWhenDwDataIsUnavailable() {
        StatSearchRequest request = request();
        when(dwStatDataProvider.selectMessageStats(request)).thenReturn(Optional.empty());
        when(dwStatDataProvider.selectDeliveryMessageStatByDegrees(request)).thenReturn(Optional.empty());
        when(dwStatDataProvider.selectActiveChannels()).thenReturn(Optional.empty());
        when(statMapper.selectMessageStats(request)).thenReturn(List.of(
                MessageStatVO.builder()
                        .date(LocalDate.of(2026, 7, 1))
                        .totalSendCount(5)
                        .totalSuccessCount(5)
                        .billingCost(BigDecimal.valueOf(500))
                        .maxCost(BigDecimal.valueOf(500))
                        .build()
        ));
        when(statMapper.selectDeliveryMessageStatByDegrees(request)).thenReturn(List.of());
        when(statMapper.selectActiveChannels()).thenReturn(List.of(channel(2L, "KAKAO")));

        StatPageResponse response = service.getDeliveryStats(request);

        assertThat(response.getCards().get(0).getValue()).isEqualTo("5");
        verify(statMapper).selectMessageStats(request);
        verify(statMapper).selectDeliveryMessageStatByDegrees(request);
    }

    private StatSearchRequest request() {
        StatSearchRequest request = new StatSearchRequest();
        request.setFrom(LocalDate.of(2026, 7, 1));
        request.setTo(LocalDate.of(2026, 7, 7));
        return request;
    }

    private ChannelVO channel(Long id, String channelType) {
        return ChannelVO.builder()
                .id(id)
                .channelType(channelType)
                .isActive(true)
                .build();
    }
}
