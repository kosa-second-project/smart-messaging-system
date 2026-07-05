package com.example.smartmessaging.service;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.queue.CampaignCommandQueueDto;
import com.example.smartmessaging.dto.queue.MessageQueueDto;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignCommandConsumer {

    private final CampaignDraftService draftService;
    private final com.example.smartmessaging.mapper.CustomerMapper customerMapper;
    private final com.example.smartmessaging.mapper.SendMapper sendMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 캠페인 초경량 명령 대기열(campaign.command.queue)에서 메시지를 꺼내와 
     * 백그라운드 스레드에서 대량의 DB 벌크 적재(Bulk Insert) 및 개별 발송 큐 Enqueue 처리를 병렬 위임합니다.
     */
    @RabbitListener(queues = RabbitMQConfig.CAMP_COMMAND_QUEUE)
    public void consumeCampaignCommand(CampaignCommandQueueDto command, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("📥 [Campaign Consumer] 백그라운드 캠페인 발송 전개 개시 - HistoryID: {}, DraftID: {}", command.getSendHistoryId(), command.getDraftId());

        try {
            // 1. Redis에서 수신 대상 ID 리스트 복원
            List<Long> customerIds = draftService.getAllIds(command.getUserId(), command.getDraftId());
            if (customerIds == null || customerIds.isEmpty()) {
                throw new IllegalStateException("수신 대상 목록이 비어 있거나 이미 만료되었습니다.");
            }

            // 2. 고객 정보 DB 일괄 조회
            List<com.example.smartmessaging.dto.vo.CustomerVO> customers = customerMapper.findByIds(customerIds);
            if (customers == null || customers.isEmpty()) {
                throw new IllegalStateException("고객 상세 정보를 DB에서 조회할 수 없습니다.");
            }

            // 3. 발송 채널 정보 해석
            Long primaryChannelId = (command.getRoutingChannelIds() != null && !command.getRoutingChannelIds().isEmpty())
                    ? command.getRoutingChannelIds().get(0)
                    : 1L;

            String channelType = (primaryChannelId == 2L) ? "KAKAO" : "SMS";
            BigDecimal cost = (primaryChannelId == 2L) ? BigDecimal.valueOf(15.0) : BigDecimal.valueOf(10.0);

            // 4. SendTargetVO 목록 일괄 생성
            List<SendTargetVO> targets = new ArrayList<>();
            for (com.example.smartmessaging.dto.vo.CustomerVO customer : customers) {
                SendTargetVO target = SendTargetVO.builder()
                        .sendHistoryId(command.getSendHistoryId())
                        .customerId(customer.getId())
                        .finalChannelId(primaryChannelId)
                        .status("PENDING")
                        .cost(cost)
                        .build();
                target.setCreatedBy(command.getUserId());
                targets.add(target);
            }

            // 5. 백그라운드 일괄 벌크 인서트 (오라클 파라미터 한계 극복을 위한 1000건 단위 분할 청크 처리)
            int chunkSize = 1000;
            for (int i = 0; i < targets.size(); i += chunkSize) {
                List<SendTargetVO> chunk = targets.subList(i, Math.min(i + chunkSize, targets.size()));
                sendMapper.insertSendTargets(chunk);
            }
            log.info("💾 [Campaign Consumer] DB 벌크 적재 완료 - 총 {} 건", targets.size());

            // 6. 각 전송 타겟별로 개별 메인 발송 대기열 Enqueue (속도 향상을 위해 대량 전송)
            for (int i = 0; i < targets.size(); i++) {
                SendTargetVO target = targets.get(i);
                com.example.smartmessaging.dto.vo.CustomerVO customer = customers.get(i);

                String recipientNo = (primaryChannelId == 3L) ? customer.getEmail() : customer.getPhone();

                MessageQueueDto queueMessage = MessageQueueDto.builder()
                        .sendTargetId(target.getId())
                        .customerId(customer.getId())
                        .recipientNo(recipientNo)
                        .channelType(channelType)
                        .title(command.getTitle())
                        .content(command.getContent())
                        .build();

                // 메인 발송 처리 큐로 고속 인큐
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.MAIN_EXCHANGE,
                        RabbitMQConfig.MAIN_ROUTING_KEY,
                        queueMessage
                );
            }

            log.info("🎉 [Campaign Consumer] 백그라운드 발송 등록 완료 및 메인 큐 전개 완료 - HistoryID: {}", command.getSendHistoryId());
            
            // 수동 ACK: 큐에서 안전하게 제거
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("❌ [Campaign Consumer] 백그라운드 전개 처리 실패 - HistoryID: {}, Error: {}", command.getSendHistoryId(), e.getMessage());
            // 실패 시 Dead Letter Queue로 이송
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
