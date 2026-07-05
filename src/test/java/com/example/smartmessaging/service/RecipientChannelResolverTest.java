package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecipientChannelResolverTest {

    private final RecipientChannelResolver resolver = new RecipientChannelResolver();

    @Test
    void 고객별_동의와_연락처가_있는_채널만_사용자_우선순위대로_계획한다() {
        List<ChannelVO> channels = List.of(
                channel(1L, "KAKAO", "53"),
                channel(2L, "EMAIL", "0.15"),
                channel(3L, "SMS", "18")
        );

        List<SendRecipientCandidateVO> candidates = List.of(
                candidate(10L, "01011112222", "all@example.com", null, 1, 1, 1),
                candidate(20L, "01022223333", "email-only@example.com", null, 0, 1, 1),
                candidate(30L, "01033334444", null, null, 0, 1, 1),
                candidate(40L, null, null, null, 1, 1, 1)
        );

        List<RecipientSendPlan> plans = resolver.resolve(
                candidates,
                channels,
                List.of("KAKAO", "EMAIL", "SMS")
        );

        assertThat(plans).hasSize(3);
        assertThat(plans.get(0).getCustomerId()).isEqualTo(10L);
        assertThat(plans.get(0).getFallbackSequence()).containsExactly("KAKAO", "EMAIL", "SMS");
        assertThat(plans.get(0).getFirstChannelId()).isEqualTo(1L);
        assertThat(plans.get(0).getEstimatedCost()).isEqualByComparingTo("53");

        assertThat(plans.get(1).getCustomerId()).isEqualTo(20L);
        assertThat(plans.get(1).getFallbackSequence()).containsExactly("EMAIL", "SMS");
        assertThat(plans.get(1).getFirstChannelId()).isEqualTo(2L);

        assertThat(plans.get(2).getCustomerId()).isEqualTo(30L);
        assertThat(plans.get(2).getFallbackSequence()).containsExactly("SMS");

        assertThat(plans)
                .extracting(RecipientSendPlan::getCustomerId)
                .doesNotContain(40L);
    }

    @Test
    void 알림톡_DB채널명은_공통작업메시지에서_KAKAO로_정규화한다() {
        List<RecipientSendPlan> plans = resolver.resolve(
                List.of(candidate(10L, "01011112222", null, null, 1, 0, 0)),
                List.of(channel(7L, "KAKAO_ALIM", "53")),
                List.of("KAKAO")
        );

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getFallbackSequence()).containsExactly("KAKAO");
        assertThat(plans.get(0).getFirstChannelId()).isEqualTo(7L);
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
