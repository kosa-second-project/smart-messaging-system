package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.StatChartResponse;
import com.example.smartmessaging.dto.response.StatPageResponse;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.CustomerStatVO;
import com.example.smartmessaging.dto.vo.MessageStatByDegreeVO;
import com.example.smartmessaging.service.repository.StatMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatServiceImplTest {

    @Test
    void customerCardsUseLatestNewCustomerSnapshotAndPeriodJoinedTotal() {
        StatMapper statMapper = mock(StatMapper.class);
        StatServiceImpl service = new StatServiceImpl(statMapper);
        StatSearchRequest request = new StatSearchRequest();

        when(statMapper.selectCustomerStats(request)).thenReturn(List.of(
                CustomerStatVO.builder()
                        .date(LocalDate.of(2026, 7, 6))
                        .totalCustomerCount(100)
                        .normalCustomerCount(80)
                        .newCustomerCount(10)
                        .joinedCustomerCount(3)
                        .build(),
                CustomerStatVO.builder()
                        .date(LocalDate.of(2026, 7, 7))
                        .totalCustomerCount(105)
                        .normalCustomerCount(82)
                        .newCustomerCount(7)
                        .joinedCustomerCount(4)
                        .build()
        ));
        when(statMapper.selectCustomerChannelConsents(request)).thenReturn(List.of());
        when(statMapper.selectActiveChannels()).thenReturn(List.of());

        StatPageResponse response = service.getCustomerStats(request);

        assertThat(response.getCards()).hasSize(4);
        assertThat(response.getCards().get(2).getTitle()).isEqualTo("신규 고객");
        assertThat(response.getCards().get(2).getValue()).isEqualTo("7");
        assertThat(response.getCards().get(2).getSubText()).isEqualTo("최근 7일 동안 가입한 고객 수");
        assertThat(response.getCards().get(3).getTitle()).isEqualTo("가입 고객");
        assertThat(response.getCards().get(3).getValue()).isEqualTo("4");
        assertThat(response.getCards().get(3).getSubText()).isEqualTo("어제 가입한 고객 수");
    }

    @Test
    void fallbackSuccessChartUsesFallbackDegreesOnlyAndKeepsLowRatesVisible() {
        StatMapper statMapper = mock(StatMapper.class);
        StatServiceImpl service = new StatServiceImpl(statMapper);
        StatSearchRequest request = new StatSearchRequest();

        when(statMapper.selectMessageStats(request)).thenReturn(List.of());
        when(statMapper.selectDeliveryMessageStatByDegrees(request)).thenReturn(List.of(
                degreeStat(1, 1L, 10, 10),
                degreeStat(2, 1L, 10, 8)
        ));
        when(statMapper.selectActiveChannels()).thenReturn(List.of(channel(1L, "SMS")));

        StatPageResponse response = service.getDeliveryStats(request);
        StatChartResponse fallbackChart = response.getCharts().stream()
                .filter(chart -> "fallbackSuccess".equals(chart.getChartId()))
                .findFirst()
                .orElseThrow();

        assertThat(fallbackChart.getLabels()).containsExactly("1차", "2차", "3차");
        assertThat(fallbackChart.getDatasets()).hasSize(1);
        assertThat(fallbackChart.getDatasets().get(0).getLabel()).isEqualTo("SMS");
        assertThat(fallbackChart.getDatasets().get(0).getData()).containsExactly(100.0, 80.0, 0.0);
    }

    private MessageStatByDegreeVO degreeStat(int degree, Long channelId, int sendCount, int successCount) {
        return MessageStatByDegreeVO.builder()
                .degree(degree)
                .channelId(channelId)
                .sendCount(sendCount)
                .successCount(successCount)
                .cost(BigDecimal.ZERO)
                .date(LocalDate.of(2026, 7, 7))
                .build();
    }

    private ChannelVO channel(Long id, String channelType) {
        return ChannelVO.builder()
                .id(id)
                .channelType(channelType)
                .build();
    }
}
