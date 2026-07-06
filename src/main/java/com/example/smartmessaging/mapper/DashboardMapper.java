package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse.QueueJob;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse.QueueStatus;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse.TemplatePerformance;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DashboardMapper {
    List<SendHistoryVO> selectRecentSends();

    List<TemplatePerformance> selectTemplatePerformanceTop(StatSearchRequest request);

    List<QueueStatus> selectSendTargetStatusCounts();

    List<QueueJob> selectQueueJobs();
}
