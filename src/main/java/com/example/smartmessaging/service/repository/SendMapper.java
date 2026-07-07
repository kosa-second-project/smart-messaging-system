package com.example.smartmessaging.service.repository;

import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SendMapper {

    /**
     * 발송 마스터 이력(send_history)을 생성합니다. (id 자동 생성 반환)
     */
    void insertSendHistory(SendHistoryVO history);

    /**
     * 개별 발송 대상(send_target)을 생성합니다. (id 자동 생성 반환)
     */
    void insertSendTarget(SendTargetVO target);

    /**
     * 대량의 발송 대상(send_target) 목록을 한 번에 벌크 인서트합니다.
     */
    void insertSendTargets(@org.apache.ibatis.annotations.Param("targets") java.util.List<SendTargetVO> targets);
}

