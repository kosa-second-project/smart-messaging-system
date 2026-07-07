package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.queue.CampaignCommandQueueDto;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.service.CampaignDraftService;
import com.example.smartmessaging.service.ChannelService;
import com.example.smartmessaging.service.RecipientChannelResolver;
import com.example.smartmessaging.service.ShortUrlService;
import com.example.smartmessaging.service.repository.SendPreparationMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignCommandConsumerTest {

    @Mock
    private CampaignDraftService draftService;

    @Mock
    private ChannelService channelService;

    @Mock
    private SendPreparationMapper sendPreparationMapper;

    @Mock
    private RecipientChannelResolver recipientChannelResolver;

    @Mock
    private ShortUrlService shortUrlService;

    @Mock
    private MessageQueuePublisher messageQueuePublisher;

    @Mock
    private Channel rabbitChannel;

    @InjectMocks
    private CampaignCommandConsumer consumer;

    @Test
    void immediateCampaign_doesNotOverwriteCompletionByUpdatingSendingAfterPublish() throws Exception {
        CampaignCommandQueueDto command = CampaignCommandQueueDto.builder()
                .sendHistoryId(10L)
                .draftId("draft-1")
                .userId(20L)
                .title("title")
                .content("content")
                .purpose("INFO")
                .templateId(30L)
                .routingChannelIds(List.of(1L))
                .build();
        SendRecipientCandidateVO recipient = recipient(100L);

        when(sendPreparationMapper.selectSendHistoryById(10L))
                .thenReturn(SendHistoryVO.builder().id(10L).status("SENDING").build());
        when(draftService.getDraftCustomerIds(20L, "draft-1")).thenReturn(List.of(100L));
        when(channelService.getActiveChannels()).thenReturn(List.of(channel(1L, "SMS")));
        when(sendPreparationMapper.findRecipientCandidatesByCustomerIds(List.of(100L))).thenReturn(List.of(recipient));
        when(recipientChannelResolver.resolve(any(), any(), any())).thenReturn(List.of(plan(recipient)));

        consumer.consumeCampaignCommand(command, rabbitChannel, 1L);

        verify(sendPreparationMapper, times(1)).updateHistoryStatus(10L, "SENDING");
        verify(messageQueuePublisher).publish(any());
        verify(rabbitChannel).basicAck(1L, false);
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
