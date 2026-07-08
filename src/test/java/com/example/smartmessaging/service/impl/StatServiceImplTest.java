package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.StatPageResponse;
import com.example.smartmessaging.dto.vo.CustomerStatVO;
import com.example.smartmessaging.service.repository.StatMapper;
import org.junit.jupiter.api.Test;

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
        assertThat(response.getCards().get(3).getValue()).isEqualTo("7");
    }
}
