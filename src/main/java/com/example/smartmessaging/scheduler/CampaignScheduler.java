package com.example.smartmessaging.scheduler;

import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.mapper.SendPreparationMapper;
import com.example.smartmessaging.service.MessageQueuePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private final SendPreparationMapper sendPreparationMapper;
    private final MessageQueuePublisher messageQueuePublisher;

    /**
     * 매 30초마다 실행되어 예약 시간이 도래한 SCHEDULED 캠페인을 감지하고 
     * 수신 대상자 리스트(st.status = 'PENDING')를 긁어와 메인 발송 큐로 순차 송출합니다.
     */
    @Scheduled(cron = "*/30 * * * * *")
    @Transactional
    public void processReservedCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        List<SendHistoryVO> pendingReservedCampaigns = sendPreparationMapper.findPendingReservedCampaigns(now);

        if (pendingReservedCampaigns.isEmpty()) {
            return;
        }

        log.info("⏰ [Campaign Scheduler] 예약 발송 대상 캠페인 {}건 감지 완료", pendingReservedCampaigns.size());

        for (SendHistoryVO campaign : pendingReservedCampaigns) {
            try {
                // 1. 캠페인 상태를 SENDING(발송중)으로 먼저 전환하여 중복 스케줄링 방지
                sendPreparationMapper.updateHistoryStatus(campaign.getId(), "SENDING");

                // 2. 예약된 라우팅 채널 우선순위 채널 ID 리스트 조회
                List<Long> routingChannelIds = sendPreparationMapper.findRoutingChannelIdsByHistoryId(campaign.getId());
                
                // 3. 해당 캠페인의 PENDING 수신 대상자 리스트 상세 조회 (Single JOIN Query)
                List<MessageTaskDto> tasks = sendPreparationMapper.selectReservedSendTasks(campaign.getId());
                
                if (tasks.isEmpty()) {
                    // 수신 대상이 없으면 즉시 완료 처리
                    sendPreparationMapper.updateHistoryStatus(campaign.getId(), "COMPLETED");
                    log.info("🚀 [Campaign Scheduler] 캠페인(HistoryID: {}) 에 PENDING 상태의 발송 대상이 없어 완료 처리했습니다.", campaign.getId());
                    continue;
                }

                // 우선순위 채널 타입 목록 조회 (가상의 시퀀스로 변환하기 위함)
                // 라우팅 채널 우선순위를 시퀀스 문자열(List<String>) 형태로 변환
                // 간단 매핑: 채널 ID 1 = "SMS", 2 = "KAKAO", 3 = "EMAIL" 등
                java.util.List<String> channelSequence = routingChannelIds.stream()
                        .map(id -> {
                            if (id == 1L) return "SMS";
                            if (id == 2L) return "KAKAO";
                            if (id == 3L) return "EMAIL";
                            return "SMS";
                        })
                        .toList();

                // 4. 개별 메시지들을 발송용 메인 큐로 송출
                int sentCount = 0;
                for (MessageTaskDto task : tasks) {
                    task.setFallbackSequence(channelSequence);
                    task.setCurrentStep(0);
                    // 단축 URL 및 080 수신거부는 이미 전개 시점(CampaignCommandConsumer)에 셋팅되었거나, 
                    // 필요 시 셋팅되어 있을 테니 큐로 그대로 발행해 줍니다.
                    messageQueuePublisher.publish(task);
                    sentCount++;
                }

                log.info("🚀 [Campaign Scheduler] 캠페인(HistoryID: {}) 발송 개시 완료 - 총 {} 건 인큐 완료", campaign.getId(), sentCount);

            } catch (Exception e) {
                log.error("❌ [Campaign Scheduler] 캠페인(HistoryID: {}) 예약 처리 중 심각한 예외 발생", campaign.getId(), e);
                // 오류 복구를 위해 대기 상태로 되돌림
                sendPreparationMapper.updateHistoryStatus(campaign.getId(), "SCHEDULED");
            }
        }
    }
}
