package com.example.smartmessaging.service;

import com.example.smartmessaging.service.repository.StatsBatchMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
public class StatsBatchService {

    // 통계 배치 전용 SQL을 호출하는 Mapper.
    private final StatsBatchMapper statsBatchMapper;

    // 배치에는 로그인 사용자가 없으므로 audit 컬럼에 넣을 시스템 사용자 ID를 설정에서 읽는다.
    private final Long systemUserId;

    public StatsBatchService(
            StatsBatchMapper statsBatchMapper,
            @Value("${stats.batch.system-user-id}") Long systemUserId
    ) {
        this.statsBatchMapper = statsBatchMapper;
        this.systemUserId = systemUserId;
    }

    /**
     * message_stat 하루치 통계를 집계한다.
     *
     * 처음 실행이면 soft-delete 대상이 없어서 0건 처리되고 새 row만 insert 된다.
     * 재실행이면 기존 활성 row를 soft-delete 한 뒤 send_history 원천 데이터로 새 row를 insert 한다.
     */
    @Transactional
    public void aggregateMessageStat(LocalDate statDate) {
        int deletedCount = statsBatchMapper.softDeleteMessageStat(statDate, systemUserId);
        int insertedCount = statsBatchMapper.insertMessageStat(statDate, systemUserId);

        log.info(
                "[StatsBatch] message_stat aggregated - statDate: {}, softDeleted: {}, inserted: {}",
                statDate,
                deletedCount,
                insertedCount
        );
    }

    /**
     * message_stat_by_degree 하루치 통계를 집계한다.
     *
     * send_attempt를 기준으로 attempt_order와 channel_id별 발송 수, 성공 수, 비용을 집계한다.
     */
    @Transactional
    public void aggregateMessageStatByDegree(LocalDate statDate) {
        int deletedCount = statsBatchMapper.softDeleteMessageStatByDegree(statDate, systemUserId);
        int insertedCount = statsBatchMapper.insertMessageStatByDegree(statDate, systemUserId);

        log.info(
                "[StatsBatch] message_stat_by_degree aggregated - statDate: {}, softDeleted: {}, inserted: {}",
                statDate,
                deletedCount,
                insertedCount
        );
    }

    /**
     * customer_stat 하루치 고객 통계를 집계한다.
     *
     * 고객 통계는 발송 이력이 아니라 customer, customer_channel_consent, channel의 현재 상태를
     * statDate 기준 스냅샷으로 저장한다.
     */
    @Transactional
    public void aggregateCustomerStat(LocalDate statDate) {
        int deletedCount = statsBatchMapper.softDeleteCustomerStat(statDate, systemUserId);
        int insertedCount = statsBatchMapper.insertCustomerStat(statDate, systemUserId);

        log.info(
                "[StatsBatch] customer_stat aggregated - statDate: {}, softDeleted: {}, inserted: {}",
                statDate,
                deletedCount,
                insertedCount
        );
    }

    /**
     * channel_stat 채널별 누적 성과 통계를 집계한다.
     *
     * channel_stat에는 stat_date 컬럼이 없기 때문에 특정 날짜 하루치가 아니라
     * 현재 원천 데이터 기준의 채널별 전체 성과 스냅샷을 다시 만든다.
     */
    @Transactional
    public void aggregateChannelStat(LocalDate statDate) {
        int deletedCount = statsBatchMapper.softDeleteChannelStat(systemUserId);
        int insertedCount = statsBatchMapper.insertChannelStat(systemUserId);

        log.info(
                "[StatsBatch] channel_stat aggregated - statDate: {}, softDeleted: {}, inserted: {}",
                statDate,
                deletedCount,
                insertedCount
        );
    }

    /**
     * click_stat 시간대별 클릭 통계를 집계한다.
     *
     * short_url.clicked_at을 기준으로 statDate 하루 동안 발생한 클릭을
     * 채널별, 시간대별로 펼쳐서 저장한다.
     */
    @Transactional
    public void aggregateClickStat(LocalDate statDate) {
        int deletedCount = statsBatchMapper.softDeleteClickStat(statDate, systemUserId);
        int insertedCount = statsBatchMapper.insertClickStat(statDate, systemUserId);

        log.info(
                "[StatsBatch] click_stat aggregated - statDate: {}, softDeleted: {}, inserted: {}",
                statDate,
                deletedCount,
                insertedCount
        );
    }

    /**
     * template_stat 하루치 템플릿별 성과 통계를 집계한다.
     *
     * send_history.template_id 기준으로 발송량을 묶고,
     * short_url 클릭/전환 이력을 합산해서 템플릿 상세와 대시보드 성과 Top 5에서 사용할 수 있게 저장한다.
     */
    @Transactional
    public void aggregateTemplateStat(LocalDate statDate) {
        int deletedCount = statsBatchMapper.softDeleteTemplateStat(statDate, systemUserId);
        int insertedCount = statsBatchMapper.insertTemplateStat(statDate, systemUserId);

        log.info(
                "[StatsBatch] template_stat aggregated - statDate: {}, softDeleted: {}, inserted: {}",
                statDate,
                deletedCount,
                insertedCount
        );
    }
}
