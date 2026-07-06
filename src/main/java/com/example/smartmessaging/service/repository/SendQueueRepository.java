package com.example.smartmessaging.service.repository;

import com.example.smartmessaging.dto.model.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendAttemptVO;
import com.example.smartmessaging.dto.vo.SendHistoryRoutingVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SendQueueRepository {
    int insertSendHistory(SendHistoryVO sendHistory);

    int insertSendHistoryRouting(SendHistoryRoutingVO routing);

    SendHistoryVO findSendHistoryById(@Param("sendHistoryId") Long sendHistoryId);

    SendTargetVO findSendTargetById(@Param("sendTargetId") Long sendTargetId);

    List<SendHistoryRoutingVO> findRoutingByHistoryId(@Param("sendHistoryId") Long sendHistoryId);

    List<ChannelVO> findChannelsByIds(@Param("channelIds") List<Long> channelIds);

    List<SendRecipientCandidateVO> findRecipientCandidatesByCustomerIds(@Param("customerIds") List<Long> customerIds);

    int insertSendTarget(SendTargetVO sendTarget);

    int insertSendAttempt(SendAttemptVO sendAttempt);

    int updateSendHistoryStatus(
            @Param("sendHistoryId") Long sendHistoryId,
            @Param("status") String status,
            @Param("updatedBy") Long updatedBy);

    int updateSendTargetStatus(
            @Param("sendTargetId") Long sendTargetId,
            @Param("status") String status,
            @Param("finalChannelId") Long finalChannelId,
            @Param("cost") java.math.BigDecimal cost,
            @Param("updatedBy") Long updatedBy);

    int refreshHistoryCounters(
            @Param("sendHistoryId") Long sendHistoryId,
            @Param("updatedBy") Long updatedBy);

    int countUnfinishedTargets(@Param("sendHistoryId") Long sendHistoryId);

    int markHistoryTerminal(
            @Param("sendHistoryId") Long sendHistoryId,
            @Param("updatedBy") Long updatedBy);

    List<SendTargetVO> findDueScheduledTargets();

    int countWhitelistedRecipient(
            @Param("channelType") String channelType,
            @Param("recipientValue") String recipientValue);
}
