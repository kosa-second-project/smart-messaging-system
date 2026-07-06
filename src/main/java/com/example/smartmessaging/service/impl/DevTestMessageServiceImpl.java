package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.DevTestSendRequest;
import com.example.smartmessaging.dto.response.DevTestSendResponse;
import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.CustomerVO;
import com.example.smartmessaging.dto.vo.SendHistoryRoutingVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.service.repository.CustomerMapper;
import com.example.smartmessaging.service.repository.SendPreparationMapper;
import com.example.smartmessaging.service.ChannelService;
import com.example.smartmessaging.service.DevTestMessageService;
import com.example.smartmessaging.service.EmailMessageService;
import com.example.smartmessaging.service.KakaoMessageService;
import com.example.smartmessaging.service.ShortUrlService;
import com.example.smartmessaging.service.SmsMessageService;
import com.example.smartmessaging.util.SmsMessageTypeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 개발/테스트 메시지 발송 서비스 구현체
 */
@Service
@Profile("local")
@RequiredArgsConstructor
public class DevTestMessageServiceImpl implements DevTestMessageService {

    private static final Long SYSTEM_ACTOR_ID = 1L;

    private final CustomerMapper customerMapper;
    private final ChannelService channelService;
    private final SendPreparationMapper sendPreparationMapper;
    private final ShortUrlService shortUrlService;
    private final SmsMessageService smsMessageService;
    private final EmailMessageService emailMessageService;
    private final KakaoMessageService kakaoMessageService;

    @Value("${app.dev-test.phone:01000000000}")
    private String testPhone;

    @Value("${app.dev-test.email:dev-test@example.com}")
    private String testEmail;

    @Value("${app.dev-test.consent-tag-ids:}")
    private String consentTagIds;

    @Override
    @Transactional
    public DevTestSendResponse sendToMe(Long userId, String userName, String kakaoAccessToken, DevTestSendRequest request) {
        requireDevTestContactConfigured();
        CustomerVO customer = ensureTestCustomer(resolveCustomerName(userName));
        List<ChannelVO> activeChannels = channelService.getActiveChannels();

        String title = personalize(request.getTitle(), customer);
        String content = personalize(request.getContent(), customer);
        String purpose = normalizePurpose(request.getPurpose());
        String buttonName = resolveButtonName(request.getLinkButtonName());
        ShortUrlPurpose linkPurpose = ShortUrlPurpose.from(request.getLinkPurpose());

        SendHistoryVO history = createHistory(userId, title, content, purpose);
        insertRoutingRows(history.getId(), activeChannels);

        ChannelVO smsChannel = findChannel(activeChannels, "SMS").orElse(null);
        ChannelVO emailChannel = findChannel(activeChannels, "EMAIL").orElse(null);
        ChannelVO kakaoChannel = findChannel(activeChannels, "KAKAO").orElse(null);

        ChannelSend sms = skipped("SMS", "카카오 테스트 발송에서는 SMS를 보내지 않습니다.");
        ChannelSend email = skipped("EMAIL", "카카오 테스트 발송에서는 이메일을 보내지 않습니다.");
        ChannelSend kakao = sendKakao(history, customer, kakaoChannel, kakaoAccessToken, title, content, buttonName, request.getLinkUrl(), linkPurpose);

        return DevTestSendResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .smsActionUrl(sms.actionUrl())
                .emailActionUrl(email.actionUrl())
                .kakaoActionUrl(kakao.actionUrl())
                .smsResult(sms.result())
                .emailResult(email.result())
                .kakaoResult(kakao.result())
                .build();
    }

    private CustomerVO ensureTestCustomer(String customerName) {
        CustomerVO customer = customerMapper.findByPhone(normalizePhone(testPhone));
        if (customer == null) {
            customer = CustomerVO.builder()
                    .phone(normalizePhone(testPhone))
                    .name(customerName)
                    .email(testEmail)
                    .birthDate(LocalDate.of(2000, 1, 1))
                    .gender("M")
                    .build();
            customer.setCreatedBy(SYSTEM_ACTOR_ID);
            customer.setUpdatedBy(SYSTEM_ACTOR_ID);
            customerMapper.insertTestCustomer(customer);
        } else {
            customer.setName(customerName);
            customer.setEmail(testEmail);
            customer.setUpdatedBy(SYSTEM_ACTOR_ID);
            customerMapper.updateTestCustomer(customer);
        }

        for (String channelType : List.of("SMS", "LMS", "EMAIL", "KAKAO", "KAKAO_ALIM")) {
            customerMapper.upsertChannelConsent(customer.getId(), channelType, SYSTEM_ACTOR_ID);
        }

        for (Long tagId : resolveConsentTagIds()) {
            customerMapper.upsertTestCustomerTag(customer.getId(), tagId, SYSTEM_ACTOR_ID);
        }

        return customerMapper.findByPhone(normalizePhone(testPhone));
    }

    private SendHistoryVO createHistory(Long userId, String title, String content, String purpose) {
        SendHistoryVO history = SendHistoryVO.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .purpose(purpose)
                .status("SCHEDULED")
                .totalTargetCount(1)
                .successCount(0)
                .failCount(0)
                .estimatedCost(BigDecimal.ZERO)
                .estimatedSaving(BigDecimal.ZERO)
                .actualCost(BigDecimal.ZERO)
                .build();
        history.setCreatedBy(userId);
        history.setUpdatedBy(userId);
        sendPreparationMapper.insertSendHistory(history);
        return history;
    }

    private void insertRoutingRows(Long sendHistoryId, List<ChannelVO> channels) {
        int order = 1;
        for (ChannelVO channel : channels) {
            SendHistoryRoutingVO routing = SendHistoryRoutingVO.builder()
                    .sendHistoryId(sendHistoryId)
                    .channelId(channel.getId())
                    .priorityOrder(order++)
                    .build();
            routing.setCreatedBy(SYSTEM_ACTOR_ID);
            routing.setUpdatedBy(SYSTEM_ACTOR_ID);
            sendPreparationMapper.insertSendHistoryRouting(routing);
        }
    }

    private ChannelSend sendSms(
            SendHistoryVO history,
            CustomerVO customer,
            ChannelVO smsChannel,
            String title,
            String content,
            String purpose,
            String buttonName,
            String originalUrl,
            ShortUrlPurpose linkPurpose
    ) {
        if (smsChannel == null) {
            return new ChannelSend(null, SendResult.fail("SMS", "CHANNEL_DISABLED", "활성화된 SMS 채널이 없습니다."));
        }

        SendTargetVO target = createTarget(history, customer, smsChannel);
        String actionUrl = shortUrlService.createTrackedUrl(target.getId(), originalUrl, linkPurpose);
        String unsubscribeUrl = "AD".equals(purpose)
                ? shortUrlService.createTrackedUrl(target.getId(), null, ShortUrlPurpose.UNSUBSCRIBE)
                : null;
        String messageType = SmsMessageTypeResolver.resolve(
                "SMS",
                title,
                SmsMessageTypeResolver.buildMessageText(content, purpose, buttonName, actionUrl, unsubscribeUrl)
        );
        SendResult result = smsMessageService.sendTextMessage(
                customer.getPhone(),
                title,
                content,
                messageType,
                purpose,
                buttonName,
                actionUrl,
                unsubscribeUrl
        );
        return new ChannelSend(actionUrl, result);
    }

    private ChannelSend skipped(String channel, String message) {
        return new ChannelSend(null, SendResult.fail(channel, "SKIPPED", message));
    }

    private ChannelSend sendEmail(
            SendHistoryVO history,
            CustomerVO customer,
            ChannelVO emailChannel,
            String title,
            String content,
            String buttonName,
            String originalUrl,
            ShortUrlPurpose linkPurpose
    ) {
        if (emailChannel == null) {
            return new ChannelSend(null, SendResult.fail("EMAIL", "CHANNEL_DISABLED", "활성화된 이메일 채널이 없습니다."));
        }

        SendTargetVO target = createTarget(history, customer, emailChannel);
        String actionUrl = shortUrlService.createTrackedUrl(target.getId(), originalUrl, linkPurpose);
        String emailContent = content + "\n\n" + buttonName + "\n" + actionUrl;
        SendResult result = emailMessageService.sendEmail(customer.getEmail(), title, emailContent);
        return new ChannelSend(actionUrl, result);
    }

    private ChannelSend sendKakao(
            SendHistoryVO history,
            CustomerVO customer,
            ChannelVO kakaoChannel,
            String kakaoAccessToken,
            String title,
            String content,
            String buttonName,
            String originalUrl,
            ShortUrlPurpose linkPurpose
    ) {
        if (kakaoChannel == null) {
            return new ChannelSend(null, SendResult.fail("KAKAO", "CHANNEL_DISABLED", "활성화된 카카오 채널이 없습니다."));
        }
        if (kakaoAccessToken == null || kakaoAccessToken.isBlank()) {
            return new ChannelSend(null, SendResult.fail("KAKAO", "KAKAO_LOGIN_REQUIRED", "카카오 나에게 보내기는 카카오 로그인이 필요합니다."));
        }

        SendTargetVO target = createTarget(history, customer, kakaoChannel);
        String actionUrl = shortUrlService.createTrackedUrl(target.getId(), originalUrl, linkPurpose);
        try {
            boolean success = kakaoMessageService.sendMemoMessage(kakaoAccessToken, title, content, buttonName, actionUrl);
            return new ChannelSend(actionUrl, success
                    ? SendResult.success("KAKAO")
                    : SendResult.fail("KAKAO", "KAKAO_SEND_FAIL", "카카오 나에게 보내기에 실패했습니다."));
        } catch (Exception e) {
            return new ChannelSend(actionUrl, SendResult.fail("KAKAO", "KAKAO_SEND_FAIL", e.getMessage()));
        }
    }

    private SendTargetVO createTarget(SendHistoryVO history, CustomerVO customer, ChannelVO channel) {
        SendTargetVO target = SendTargetVO.builder()
                .sendHistoryId(history.getId())
                .customerId(customer.getId())
                .finalChannelId(channel.getId())
                .status("PENDING")
                .cost(channel.getCostPerMsg() == null ? BigDecimal.ZERO : channel.getCostPerMsg())
                .userUuid(createTrackingUserUuid())
                .build();
        target.setCreatedBy(history.getUserId());
        target.setUpdatedBy(history.getUserId());
        sendPreparationMapper.insertSendTarget(target);
        return target;
    }

    private Optional<ChannelVO> findChannel(List<ChannelVO> channels, String channelType) {
        String target = normalizeChannelType(channelType);
        return channels.stream()
                .filter(Objects::nonNull)
                .filter(channel -> target.equals(normalizeChannelType(channel.getChannelType())))
                .findFirst();
    }

    private String personalize(String value, CustomerVO customer) {
        String text = value == null ? "" : value;
        String name = customer.getName() == null ? "" : customer.getName();
        return text.replace("#{고객명}", name).replace("#{이름}", name);
    }

    private String resolveButtonName(String linkButtonName) {
        if (linkButtonName == null || linkButtonName.isBlank()) {
            return "자세히 보기";
        }
        return linkButtonName.trim();
    }

    private String normalizePurpose(String purpose) {
        String normalized = purpose == null ? "INFO" : purpose.trim().toUpperCase(Locale.ROOT);
        if ("INFORMATIONAL".equals(normalized)) {
            return "INFO";
        }
        return normalized;
    }

    private String normalizeChannelType(String channelType) {
        if (channelType == null) {
            return "";
        }
        String normalized = channelType.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("KAKAO")) {
            return "KAKAO";
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }

    private String resolveCustomerName(String userName) {
        if (userName == null || userName.isBlank()) {
            return "테스트 고객";
        }
        return userName.trim();
    }

    private void requireDevTestContactConfigured() {
        if (normalizePhone(testPhone).isBlank() || testEmail == null || testEmail.isBlank()) {
            throw new IllegalStateException("로컬 테스트 발송용 app.dev-test.phone/email 설정이 필요합니다.");
        }
    }

    private List<Long> resolveConsentTagIds() {
        if (consentTagIds == null || consentTagIds.isBlank()) {
            return List.of();
        }
        return List.of(consentTagIds.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::parseLong)
                .toList();
    }

    private String createTrackingUserUuid() {
        return UUID.randomUUID().toString();
    }

    private record ChannelSend(String actionUrl, SendResult result) {
    }
}
