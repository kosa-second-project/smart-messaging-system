package com.example.smartmessaging.scheduler;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.service.ChannelService;
import com.example.smartmessaging.service.RecipientChannelResolver;
import com.example.smartmessaging.service.ShortUrlService;
import com.example.smartmessaging.service.queue.MessageQueuePublisher;
import com.example.smartmessaging.service.repository.HistoryMapper;
import com.example.smartmessaging.service.repository.SendPreparationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignSchedulerTest {

    @Mock
    private SendPreparationMapper sendPreparationMapper;

    @Mock
    private HistoryMapper historyMapper;

    @Mock
    private ChannelService channelService;

    @Mock
    private RecipientChannelResolver recipientChannelResolver;

    @Mock
    private ShortUrlService shortUrlService;

    @Mock
    private MessageQueuePublisher messageQueuePublisher;

    @InjectMocks
    private CampaignScheduler scheduler;

    @Test
    void reservedCampaign_doesNotUpdateSendingAgainAfterPublishingTargets() {
        SendHistoryVO campaign = SendHistoryVO.builder()
                .id(10L)
                .templateId(30L)
                .userId(20L)
                .title("title")
                .content("content")
                .purpose("INFO")
                .status("SCHEDULED")
                .build();
        SendTargetVO target = SendTargetVO.builder()
                .id(40L)
                .sendHistoryId(10L)
                .customerId(100L)
                .status("PENDING")
                .build();
        SendRecipientCandidateVO recipient = recipient(100L);

        when(sendPreparationMapper.findPendingReservedCampaigns(any(LocalDateTime.class))).thenReturn(List.of(campaign));
        when(sendPreparationMapper.selectPendingTargetsByHistoryId(10L)).thenReturn(List.of(target));
        when(channelService.getActiveChannels()).thenReturn(List.of(channel(1L, "SMS")));
        when(sendPreparationMapper.findRoutingChannelIdsByHistoryId(10L)).thenReturn(List.of(1L));
        when(sendPreparationMapper.findRecipientCandidatesByCustomerIds(List.of(100L))).thenReturn(List.of(recipient));
        when(recipientChannelResolver.resolve(any(), any(), any())).thenReturn(List.of(plan(recipient)));

        scheduler.processReservedCampaigns();

        verify(sendPreparationMapper, times(1)).updateHistoryStatus(10L, "SENDING");
        verify(sendPreparationMapper, never()).updateHistoryStatus(10L, "SENT");
        verify(messageQueuePublisher).publish(any());
    }

    private ChannelVO channel(Long id, String channelType) {
        return ChannelVO.builder()
                .id(id)
                .channelType(channelType)
                .costPerMsg(BigDecimal.ZERO)
                .isActive(true)
                .build();
    }

    private SendRecipientCandidateVO recipient(Long customerId) {
        SendRecipientCandidateVO recipient = new SendRecipientCandidateVO();
        recipient.setCustomerId(customerId);
        recipient.setIsRealCustomer(false);
        recipient.setPhone("01011112222");
        recipient.setSmsConsent(1);
        return recipient;
    }

    private RecipientSendPlan plan(SendRecipientCandidateVO recipient) {
        return RecipientSendPlan.builder()
                .customerId(recipient.getCustomerId())
                .firstChannelId(1L)
                .estimatedCost(BigDecimal.ZERO)
                .fallbackSequence(List.of("SMS"))
                .recipient(recipient)
                .build();
    }
}
