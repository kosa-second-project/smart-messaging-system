package com.example.smartmessaging.service.repository;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse.TemplatePerformance;
import com.example.smartmessaging.dto.vo.CustomerStatVO;
import com.example.smartmessaging.dto.vo.MessageStatByDegreeVO;
import com.example.smartmessaging.dto.vo.MessageStatVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DashboardMapper {
    MessageStatVO selectMessageSummary(StatSearchRequest request);

    List<MessageStatVO> selectMessageTrend(StatSearchRequest request);

    List<MessageStatByDegreeVO> selectChannelSendSummary(StatSearchRequest request);

    CustomerStatVO selectLatestCustomerStat(StatSearchRequest request);

    List<SendHistoryVO> selectRecentSends();

    List<TemplatePerformance> selectTemplatePerformanceTop(StatSearchRequest request);
}
