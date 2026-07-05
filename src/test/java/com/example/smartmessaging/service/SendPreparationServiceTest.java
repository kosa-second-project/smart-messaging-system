package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.SendPrepareRequestDTO;
import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.response.SendPrepareResponseDTO;
import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendHistoryRoutingVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.mapper.SendPreparationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendPreparationServiceTest {

    private CampaignDraftService draftService;
    private ChannelService channelService;
    private SendPreparationMapper sendPreparationMapper;
    private MessageQueuePublisher messageQueuePublisher;
    private ShortUrlService shortUrlService;
    private SendPreparationService sendPreparationService;

    @BeforeEach
    void setUp() {
        draftService = mock(CampaignDraftService.class);
        channelService = mock(ChannelService.class);
        sendPreparationMapper = mock(SendPreparationMapper.class);
        messageQueuePublisher = mock(MessageQueuePublisher.class);
        shortUrlService = mock(ShortUrlService.class);
        sendPreparationService = new SendPreparationService(
                draftService,
                channelService,
                sendPreparationMapper,
                new RecipientChannelResolver(),
                messageQueuePublisher,
                shortUrlService
        );
    }

    @Test
    void 기존DB구조에_발송요청을_저장하고_공통작업메시지를_발행한다() {
        SendPrepareRequestDTO request = new SendPrepareRequestDTO();
        request.setDraftId("draft-1");
        request.setTemplateId(7L);
        request.setTitle("쿠폰 안내");
        request.setContent("쿠폰이 도착했습니다.");
        request.setPurpose("AD");
        request.setPriorities(List.of("KAKAO", "EMAIL", "SMS"));
        request.setLinkButtonName("쿠폰 보기");
        request.setLinkUrl("https://example.com/coupon");
        request.setLinkPurpose("PURCHASE");

        when(draftService.getDraftCustomerIds(10L, "draft-1")).thenReturn(List.of(100L, 200L));
        when(channelService.getActiveChannels()).thenReturn(List.of(
                channel(1L, "KAKAO", "53"),
                channel(2L, "EMAIL", "0.15"),
                channel(3L, "SMS", "18")
        ));
        when(sendPreparationMapper.findRecipientCandidatesByCustomerIds(List.of(100L, 200L))).thenReturn(List.of(
                candidate(100L, "01011112222", "all@example.com", "kakao-100", 1, 1, 1),
                candidate(200L, "01022223333", "email@example.com", null, 0, 1, 1)
        ));
        doAnswer(invocation -> {
            SendHistoryVO history = invocation.getArgument(0);
            history.setId(900L);
            return 1;
        }).when(sendPreparationMapper).insertSendHistory(any(SendHistoryVO.class));
        doAnswer(invocation -> {
            SendTargetVO target = invocation.getArgument(0);
            target.setId(target.getCustomerId() + 1000);
            return 1;
        }).when(sendPreparationMapper).insertSendTarget(any(SendTargetVO.class));
        when(shortUrlService.createTrackedUrl(1100L, "https://example.com/coupon", ShortUrlPurpose.PURCHASE))
                .thenReturn("http://localhost:8080/r/purchase100");
        when(shortUrlService.createTrackedUrl(1200L, "https://example.com/coupon", ShortUrlPurpose.PURCHASE))
                .thenReturn("http://localhost:8080/r/purchase200");
        when(shortUrlService.createTrackedUrl(1100L, null, ShortUrlPurpose.UNSUBSCRIBE))
                .thenReturn("http://localhost:8080/u/unsub100");
        when(shortUrlService.createTrackedUrl(1200L, null, ShortUrlPurpose.UNSUBSCRIBE))
                .thenReturn("http://localhost:8080/u/unsub200");

        SendPrepareResponseDTO response = sendPreparationService.prepare(10L, request);

        ArgumentCaptor<SendHistoryVO> historyCaptor = ArgumentCaptor.forClass(SendHistoryVO.class);
        verify(sendPreparationMapper).insertSendHistory(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getId()).isEqualTo(900L);
        assertThat(historyCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo("SCHEDULED");
        assertThat(historyCaptor.getValue().getTotalTargetCount()).isEqualTo(2);
        assertThat(historyCaptor.getValue().getEstimatedCost()).isEqualByComparingTo("53.15");

        ArgumentCaptor<SendHistoryRoutingVO> routingCaptor = ArgumentCaptor.forClass(SendHistoryRoutingVO.class);
        verify(sendPreparationMapper, org.mockito.Mockito.times(3)).insertSendHistoryRouting(routingCaptor.capture());
        assertThat(routingCaptor.getAllValues())
                .extracting(SendHistoryRoutingVO::getChannelId, SendHistoryRoutingVO::getPriorityOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, 1),
                        org.assertj.core.groups.Tuple.tuple(2L, 2),
                        org.assertj.core.groups.Tuple.tuple(3L, 3)
                );

        ArgumentCaptor<MessageTaskDto> taskCaptor = ArgumentCaptor.forClass(MessageTaskDto.class);
        verify(messageQueuePublisher, org.mockito.Mockito.times(2)).publish(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues())
                .extracting(MessageTaskDto::getSendHistoryId, MessageTaskDto::getCustomerId, MessageTaskDto::getFallbackSequence)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(900L, 100L, List.of("KAKAO", "EMAIL", "SMS")),
                        org.assertj.core.groups.Tuple.tuple(900L, 200L, List.of("EMAIL", "SMS"))
                );
        assertThat(taskCaptor.getAllValues())
                .extracting(MessageTaskDto::getActionButtonName, MessageTaskDto::getActionUrl, MessageTaskDto::getUnsubscribeUrl)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("쿠폰 보기", "http://localhost:8080/r/purchase100", "http://localhost:8080/u/unsub100"),
                        org.assertj.core.groups.Tuple.tuple("쿠폰 보기", "http://localhost:8080/r/purchase200", "http://localhost:8080/u/unsub200")
                );

        assertThat(response.getSendHistoryId()).isEqualTo(900L);
        assertThat(response.getTotalRequestedCount()).isEqualTo(2);
        assertThat(response.getPreparedTargetCount()).isEqualTo(2);
        assertThat(response.getExcludedTargetCount()).isZero();
        verify(draftService).deleteDraft(10L, "draft-1");
    }

    private ChannelVO channel(Long id, String type, String cost) {
        return ChannelVO.builder()
                .id(id)
                .channelType(type)
                .costPerMsg(new BigDecimal(cost))
                .isActive(true)
                .build();
    }

    private SendRecipientCandidateVO candidate(
            Long customerId,
            String phone,
            String email,
            String kakaoUserKey,
            int kakaoConsent,
            int emailConsent,
            int smsConsent
    ) {
        SendRecipientCandidateVO candidate = new SendRecipientCandidateVO();
        candidate.setCustomerId(customerId);
        candidate.setPhone(phone);
        candidate.setEmail(email);
        candidate.setKakaoUserKey(kakaoUserKey);
        candidate.setKakaoConsent(kakaoConsent);
        candidate.setEmailConsent(emailConsent);
        candidate.setSmsConsent(smsConsent);
        return candidate;
    }
}
