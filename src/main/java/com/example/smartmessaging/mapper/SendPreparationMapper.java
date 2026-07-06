package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.vo.SendHistoryRoutingVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SendPreparationMapper {
    List<SendRecipientCandidateVO> findRecipientCandidatesByCustomerIds(@Param("customerIds") List<Long> customerIds);

    int insertSendHistory(SendHistoryVO sendHistory);

    int insertSendHistoryRouting(SendHistoryRoutingVO routing);

    int insertSendTarget(SendTargetVO target);

    SendHistoryVO selectSendHistoryById(@Param("id") Long id);

    void updateHistoryStatus(@Param("id") Long id, @Param("status") String status);

    List<SendHistoryVO> findPendingReservedCampaigns(@Param("now") java.time.LocalDateTime now);

    List<Long> findRoutingChannelIdsByHistoryId(@Param("sendHistoryId") Long sendHistoryId);

    List<SendTargetVO> selectPendingTargetsByHistoryId(@Param("sendHistoryId") Long sendHistoryId);

    List<com.example.smartmessaging.dto.request.MessageTaskDto> selectReservedSendTasks(@Param("sendHistoryId") Long sendHistoryId);
}
