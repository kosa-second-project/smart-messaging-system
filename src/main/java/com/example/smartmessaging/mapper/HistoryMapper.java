package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryAttemptFlowResponseDTO;
import com.example.smartmessaging.dto.response.HistoryChannelResponseDTO;
import com.example.smartmessaging.dto.response.HistoryDetailResponseDTO;
import com.example.smartmessaging.dto.response.HistoryFilterOptionDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.HistoryTagResponseDTO;
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

    /**
     * 발송 대상(send_target)의 최종 상태를 업데이트합니다.
     */
    void updateSendTargetStatus(@Param("sendTargetId") Long sendTargetId, @Param("status") String status);

    void incrementSuccessCount(@Param("sendHistoryId") Long sendHistoryId);
    void incrementFailCount(@Param("sendHistoryId") Long sendHistoryId);
    void updateHistoryStatus(@Param("sendHistoryId") Long sendHistoryId, @Param("status") String status);
}
