package com.example.smartmessaging.service.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 통계 배치에서 사용하는 MyBatis Mapper.
 *
 * 화면 조회용 StatMapper와 분리해서, 배치가 통계 테이블을 집계할 때 사용하는
 * 변경 SQL만 이 Mapper에 둔다.
 */
@Mapper
public interface StatsBatchMapper {

    /**
     * 특정 통계일자의 기존 message_stat 활성 row를 soft-delete 처리한다.
     *
     * DB unique index는 is_deleted = 0인 stat_date에만 적용되므로,
     * 기존 활성 row를 비활성화한 뒤 같은 날짜의 새 활성 row를 insert할 수 있다.
     */
    int softDeleteMessageStat(
            @Param("statDate") LocalDate statDate,
            @Param("systemUserId") Long systemUserId
    );

    /**
     * send_history 데이터를 기준으로 특정 날짜의 message_stat row를 insert 한다.
     */
    int insertMessageStat(
            @Param("statDate") LocalDate statDate,
            @Param("systemUserId") Long systemUserId
    );

    /**
     * 특정 통계일자의 기존 message_stat_by_degree 활성 row를 soft-delete 처리한다.
     */
    int softDeleteMessageStatByDegree(
            @Param("statDate") LocalDate statDate,
            @Param("systemUserId") Long systemUserId
    );

    /**
     * send_attempt 데이터를 기준으로 특정 날짜의 차수/채널별 message_stat_by_degree row를 insert 한다.
     */
    int insertMessageStatByDegree(
            @Param("statDate") LocalDate statDate,
            @Param("systemUserId") Long systemUserId
    );

    /**
     * 특정 통계일자의 기존 customer_stat 활성 row를 soft-delete 처리한다.
     */
    int softDeleteCustomerStat(
            @Param("statDate") LocalDate statDate,
            @Param("systemUserId") Long systemUserId
    );

    /**
     * customer, customer_channel_consent, channel 데이터를 기준으로 특정 날짜의 customer_stat row를 insert 한다.
     */
    int insertCustomerStat(
            @Param("statDate") LocalDate statDate,
            @Param("systemUserId") Long systemUserId
    );

    /**
     * 기존 channel_stat 활성 row를 전부 soft-delete 처리한다.
     */
    int softDeleteChannelStat(
            @Param("systemUserId") Long systemUserId
    );

    /**
     * 채널별 클릭, 전환, 수신 동의 누적 성과를 channel_stat에 insert 한다.
     */
    int insertChannelStat(
            @Param("systemUserId") Long systemUserId
    );

    /**
     * 특정 통계일자의 기존 click_stat 활성 row를 soft-delete 처리한다.
     */
    int softDeleteClickStat(
            @Param("statDate") LocalDate statDate,
            @Param("systemUserId") Long systemUserId
    );

    /**
     * short_url 클릭 이력을 기준으로 특정 날짜의 채널별 시간대 클릭 통계를 insert 한다.
     */
    int insertClickStat(
            @Param("statDate") LocalDate statDate,
            @Param("systemUserId") Long systemUserId
    );
}
