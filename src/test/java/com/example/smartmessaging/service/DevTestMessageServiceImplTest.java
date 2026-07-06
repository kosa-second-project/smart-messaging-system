package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.DevTestSendRequest;
import com.example.smartmessaging.dto.response.DevTestSendResponse;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.CustomerVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.service.impl.DevTestMessageServiceImpl;
import com.example.smartmessaging.service.repository.CustomerMapper;
import com.example.smartmessaging.service.repository.SendPreparationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DevTestMessageServiceImplTest {

    private final CustomerMapper customerMapper = mock(CustomerMapper.class);
    private final ChannelService channelService = mock(ChannelService.class);
    private final SendPreparationMapper sendPreparationMapper = mock(SendPreparationMapper.class);
    private final ShortUrlService shortUrlService = mock(ShortUrlService.class);
    private final SmsMessageService smsMessageService = mock(SmsMessageService.class);
    private final EmailMessageService emailMessageService = mock(EmailMessageService.class);
    private final KakaoMessageService kakaoMessageService = mock(KakaoMessageService.class);

    private DevTestMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DevTestMessageServiceImpl(
                customerMapper,
                channelService,
                sendPreparationMapper,
                shortUrlService,
                smsMessageService,
                emailMessageService,
                kakaoMessageService
        );
        ReflectionTestUtils.setField(service, "testPhone", "010-1234-5678");
        ReflectionTestUtils.setField(service, "testEmail", "me@example.com");
        ReflectionTestUtils.setField(service, "consentTagIds", "");
    }

    @Test
    @DisplayName("로컬 테스트 발송은 활성화된 이메일 채널로 실제 이메일 발송을 시도한다")
    void sendToMe_sendsEmailWhenEmailChannelIsActive() {
        DevTestSendRequest request = new DevTestSendRequest();
        request.setTitle("Hello #{이름}");
        request.setContent("Body #{이름}");
        request.setPurpose("INFO");
        request.setLinkButtonName("Read more");
        request.setLinkUrl("https://example.com/promo");
        request.setLinkPurpose("PURCHASE");

        CustomerVO customer = CustomerVO.builder()
                .id(100L)
                .phone("01012345678")
                .name("Tester")
                .email("old@example.com")
                .build();
        when(customerMapper.findByPhone("01012345678")).thenReturn(customer);

        ChannelVO emailChannel = ChannelVO.builder()
                .id(2L)
                .channelType("EMAIL")
                .costPerMsg(BigDecimal.ZERO)
                .isActive(true)
                .build();
        when(channelService.getActiveChannels()).thenReturn(List.of(emailChannel));

        doAnswer(invocation -> {
            SendHistoryVO history = invocation.getArgument(0);
            history.setId(10L);
            return 1;
        }).when(sendPreparationMapper).insertSendHistory(any(SendHistoryVO.class));
        doAnswer(invocation -> {
            SendTargetVO target = invocation.getArgument(0);
            target.setId(20L);
            return 1;
        }).when(sendPreparationMapper).insertSendTarget(any(SendTargetVO.class));
        when(shortUrlService.createTrackedUrl(20L, "https://example.com/promo", com.example.smartmessaging.dto.type.ShortUrlPurpose.PURCHASE))
                .thenReturn("https://kosa.kr/r/test");
        when(emailMessageService.sendEmail(
                "me@example.com",
                "Hello Tester",
                "Body Tester\n\nRead more\nhttps://kosa.kr/r/test"
        )).thenReturn(SendResult.success("EMAIL"));

        DevTestSendResponse response = service.sendToMe(1L, "Tester", null, request);

        assertThat(response.getEmail()).isEqualTo("me@example.com");
        assertThat(response.getEmailActionUrl()).isEqualTo("https://kosa.kr/r/test");
        assertThat(response.getEmailResult().isSuccess()).isTrue();
        verify(emailMessageService).sendEmail(
                "me@example.com",
                "Hello Tester",
                "Body Tester\n\nRead more\nhttps://kosa.kr/r/test"
        );
        verifyNoInteractions(smsMessageService, kakaoMessageService);
    }
}
