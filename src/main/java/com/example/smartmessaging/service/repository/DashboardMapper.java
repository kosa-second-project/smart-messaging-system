package com.example.smartmessaging.service.repository;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse.TemplatePerformance;
import com.example.smartmessaging.dto.vo.CustomerStatVO;
import com.example.smartmessaging.dto.vo.MessageStatVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DashboardMapper {
    MessageStatVO selectMessageSummary(StatSearchRequest request);

    MessageStatVO selectTodayRealtimeMessageSummary();

    List<MessageStatVO> selectMessageTrend(StatSearchRequest request);

    CustomerStatVO selectLatestCustomerStat(StatSearchRequest request);

    CustomerStatVO selectRealtimeCustomerSummary();

    List<SendHistoryVO> selectRecentSends();

    List<TemplatePerformance> selectTemplatePerformanceTop(StatSearchRequest request);

    long countActiveSendBacklog();
}
