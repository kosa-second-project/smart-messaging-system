package com.example.smartmessaging.service.repository;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryAttemptFlowResponseDTO;
import com.example.smartmessaging.dto.response.HistoryChannelResponseDTO;
import com.example.smartmessaging.dto.response.HistoryDetailResponseDTO;
import com.example.smartmessaging.dto.response.HistoryFilterOptionDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.HistoryTagResponseDTO;
import com.example.smartmessaging.dto.vo.SendAttemptVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HistoryMapper {
    long countHistories(HistorySearchRequestDTO condition);
    List<HistoryListResponseDTO> findHistories(HistorySearchRequestDTO condition);
    HistoryDetailResponseDTO findHistoryDetailById(@Param("sendHistoryId") Long sendHistoryId);
    List<HistoryAttemptFlowResponseDTO> findAttemptFlowsByHistoryId(@Param("sendHistoryId") Long sendHistoryId);
    List<HistoryChannelResponseDTO> findChannelsByHistoryIds(@Param("historyIds") List<Long> historyIds);
    List<HistoryTagResponseDTO> findTagsByHistoryIds(@Param("historyIds") List<Long> historyIds);
    List<HistoryFilterOptionDTO> findChannelOptions();
    List<HistoryFilterOptionDTO> findTagOptions();
    List<String> findPurposeOptions();

    void updateSendTargetStatus(@Param("sendTargetId") Long sendTargetId, @Param("status") String status);
    void updateSendTargetSuccess(@Param("sendTargetId") Long sendTargetId, @Param("channelId") Long channelId);
    List<SendTargetVO> findFailedTargetsForRetry(@Param("sendHistoryId") Long sendHistoryId, @Param("limit") int limit);
    void updateSendTargetStatusByIds(@Param("targetIds") List<Long> targetIds, @Param("status") String status);
    void decrementFailCount(@Param("sendHistoryId") Long sendHistoryId, @Param("count") int count);
    int findNextAttemptOrder(@Param("sendTargetId") Long sendTargetId);
    void insertSendAttempt(SendAttemptVO attempt);
    int countUnfinishedTargets(@Param("sendHistoryId") Long sendHistoryId);
    void incrementSuccessCount(@Param("sendHistoryId") Long sendHistoryId);
    void incrementActualCostByChannel(@Param("sendHistoryId") Long sendHistoryId, @Param("channelId") Long channelId);
    void incrementFailCount(@Param("sendHistoryId") Long sendHistoryId);
    void updateHistoryStatus(@Param("sendHistoryId") Long sendHistoryId, @Param("status") String status);
    void finalizeSendHistory(@Param("sendHistoryId") Long sendHistoryId, @Param("status") String status);
}
