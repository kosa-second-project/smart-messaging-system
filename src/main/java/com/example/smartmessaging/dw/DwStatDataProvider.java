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

import java.util.List;
import java.util.Optional;

public interface DwStatDataProvider {

    Optional<List<ChannelVO>> selectActiveChannels();

    Optional<List<MessageStatVO>> selectMessageStats(StatSearchRequest request);

    Optional<List<MessageStatByDegreeVO>> selectDeliveryMessageStatByDegrees(StatSearchRequest request);

    Optional<List<ChannelStatVO>> selectChannelStats(StatSearchRequest request);

    Optional<List<CustomerStatVO>> selectCustomerStats(StatSearchRequest request);

    Optional<List<CustomerChannelConsentSummaryVO>> selectCustomerChannelConsents(StatSearchRequest request);

    Optional<List<ClickStatVO>> selectPerformanceClickStats(StatSearchRequest request);

    Optional<MessageStatVO> selectMessageSummary(StatSearchRequest request);

    Optional<List<MessageStatVO>> selectMessageTrend(StatSearchRequest request);

    Optional<List<MessageStatByDegreeVO>> selectChannelSendSummary(StatSearchRequest request);

    Optional<CustomerStatVO> selectLatestCustomerStat(StatSearchRequest request);

    Optional<List<DashboardSummaryResponse.TemplatePerformance>> selectTemplatePerformanceTop(StatSearchRequest request);
}
