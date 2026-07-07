package com.example.smartmessaging.dw;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse;
import com.example.smartmessaging.dto.vo.ChannelStatVO;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.ClickStatVO;
import com.example.smartmessaging.dto.vo.CustomerChannelConsentSummaryVO;
import com.example.smartmessaging.dto.vo.CustomerStatVO;
import com.example.smartmessaging.dto.vo.MessageStatByDegreeVO;
import com.example.smartmessaging.dto.vo.MessageStatVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "dw.bigquery", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopDwStatDataProvider implements DwStatDataProvider {

    @Override
    public Optional<List<ChannelVO>> selectActiveChannels() {
        return Optional.empty();
    }

    @Override
    public Optional<List<MessageStatVO>> selectMessageStats(StatSearchRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<List<MessageStatByDegreeVO>> selectDeliveryMessageStatByDegrees(StatSearchRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<List<ChannelStatVO>> selectChannelStats(StatSearchRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<List<CustomerStatVO>> selectCustomerStats(StatSearchRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<List<CustomerChannelConsentSummaryVO>> selectCustomerChannelConsents(StatSearchRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<List<ClickStatVO>> selectPerformanceClickStats(StatSearchRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<MessageStatVO> selectMessageSummary(StatSearchRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<List<MessageStatVO>> selectMessageTrend(StatSearchRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<List<MessageStatByDegreeVO>> selectChannelSendSummary(StatSearchRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<CustomerStatVO> selectLatestCustomerStat(StatSearchRequest request) {
        return Optional.empty();
    }

    @Override
    public Optional<List<DashboardSummaryResponse.TemplatePerformance>> selectTemplatePerformanceTop(StatSearchRequest request) {
        return Optional.empty();
    }
}
