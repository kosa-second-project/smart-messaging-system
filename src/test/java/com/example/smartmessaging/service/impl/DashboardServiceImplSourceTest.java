package com.example.smartmessaging.service.impl;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceImplSourceTest {

    @Test
    void 대시보드는_통계페이지용_전체목록조회_대신_전용요약조회를_사용한다() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/example/smartmessaging/service/impl/DashboardServiceImpl.java"
        ));

        assertThat(source)
                .contains(
                        "dashboardMapper.selectTodayRealtimeMessageSummary()",
                        "dashboardMapper.selectMessageTrend(request)",
                        "dashboardMapper.selectRealtimeCustomerSummary()"
                )
                .doesNotContain(
                        "dashboardMapper.selectChannelSendSummary(request)",
                        "statMapper.selectActiveChannels()",
                        "statMapper.selectMessageStats(request)",
                        "statMapper.selectDeliveryMessageStatByDegrees(request)",
                        "statMapper.selectCustomerStats(request)"
                );
    }
}
