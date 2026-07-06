package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.client.GeminiSuggestionClient;
import com.example.smartmessaging.ai.dto.request.AiSuggestionRequest;
import com.example.smartmessaging.ai.dto.response.AiSuggestionItem;
import com.example.smartmessaging.ai.dto.response.AiSuggestionResponse;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.TemplateCategory;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSuggestionServiceTest {

    private GeminiSuggestionClient geminiSuggestionClient;
    private AiSuggestionService service;

    @BeforeEach
    void setUp() {
        geminiSuggestionClient = mock(GeminiSuggestionClient.class);
        service = new AiSuggestionService(geminiSuggestionClient, new RuleValidationService());
    }

    @Test
    void 통과한_후보를_모델_순서대로_최대_3개_반환한다() {
        AiSuggestionItem first = validAd("첫 번째");
        AiSuggestionItem rejected = new AiSuggestionItem("제외", "(광고) 문의 test@example.com 무료수신거부 080-000-0000");
        AiSuggestionItem second = validAd("두 번째");
        AiSuggestionItem third = validAd("세 번째");
        AiSuggestionItem fourth = validAd("네 번째");
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(first, rejected, second, third, fourth)
        );

        AiSuggestionResponse result = service.suggest(request(MessageType.AD));

        assertThat(result.getSuggestions())
                .extracting(AiSuggestionItem::getTitle)
                .containsExactly("첫 번째", "두 번째", "세 번째");
        verify(geminiSuggestionClient, times(1)).generate(anyString());
    }

    @Test
    void 통과_후보가_1개이면_재시도하지_않고_반환한다() {
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(
                        validAd("통과"),
                        new AiSuggestionItem("제외", "광고 표기 없음")
                )
        );

        AiSuggestionResponse result = service.suggest(request(MessageType.AD));

        assertThat(result.getSuggestions()).hasSize(1);
        verify(geminiSuggestionClient, times(1)).generate(anyString());
    }

    @Test
    void 통과_후보가_0개이면_실패_ruleId를_반영해_재시도한다() {
        when(geminiSuggestionClient.generate(anyString()))
                .thenReturn(response(new AiSuggestionItem("제외", "광고 표기 없음")))
                .thenReturn(response(validAd("재시도 통과")));

        AiSuggestionResponse result = service.suggest(request(MessageType.AD));

        assertThat(result.getSuggestions()).hasSize(1);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiSuggestionClient, times(2)).generate(promptCaptor.capture());
        assertThat(promptCaptor.getAllValues().get(1))
                .contains("MISSING_AD_PREFIX", "MISSING_OPT_OUT");
    }

    @Test
    void 총_3회_모두_통과_후보가_없으면_422_예외을_발생시킨다() {
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(new AiSuggestionItem("제외", "광고 표기 없음"))
        );

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.suggest(request(MessageType.AD)))
                .withMessage("조건을 만족하는 추천 문구를 생성하지 못했습니다.")
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.AI_SUGGESTION_NO_VALID_CANDIDATE));
        verify(geminiSuggestionClient, times(3)).generate(anyString());
    }

    @Test
    void availableVariables가_없으면_기본_3종을_프롬프트와_검증에_사용한다() {
        AiSuggestionRequest request = request(MessageType.INFO);
        request.setAvailableVariables(null);
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(new AiSuggestionItem("배송 안내", "#{고객명}님, #{주문번호} 주문이 출고되었습니다."))
        );

        AiSuggestionResponse result = service.suggest(request);

        assertThat(result.getSuggestions()).hasSize(1);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiSuggestionClient).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("#{고객명}", "#{주문번호}", "#{쿠폰명}");
    }

    @Test
    void customerTags가_없어도_추천_문구를_생성한다() {
        AiSuggestionRequest request = request(MessageType.INFO);
        request.setCustomerTags(null);
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(new AiSuggestionItem("배송 안내", "#{고객명}님, 주문 상품이 출고되었습니다."))
        );

        AiSuggestionResponse result = service.suggest(request);

        assertThat(result.getSuggestions()).hasSize(1);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiSuggestionClient).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("고객 태그: []");
    }

    @Test
    void 사용자가_지정한_변수만_허용한다() {
        AiSuggestionRequest request = request(MessageType.INFO);
        request.setAvailableVariables(List.of("#{고객명}"));
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(new AiSuggestionItem("안내", "#{고객명}님, 배송이 시작되었습니다."))
        );

        AiSuggestionResponse result = service.suggest(request);

        assertThat(result.getSuggestions()).hasSize(1);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiSuggestionClient).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("허용 변수: [#{고객명}]");
    }

    @Test
    void 프로젝트가_지원하지_않는_변수는_400으로_거부한다() {
        AiSuggestionRequest request = request(MessageType.INFO);
        request.setAvailableVariables(List.of("#{만료일}"));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.suggest(request))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );
        verify(geminiSuggestionClient, never()).generate(anyString());
    }

    @Test
    void AD와_INFO_규칙과_템플릿_카테고리를_프롬프트에_반영한다() {
        AiSuggestionRequest adRequest = request(MessageType.AD);
        adRequest.setContextType(AiContextType.TEMPLATE_CREATE);
        adRequest.setCategory(TemplateCategory.BENEFIT);
        AiSuggestionRequest infoRequest = request(MessageType.INFO);

        String adPrompt = service.buildPrompt(adRequest, List.of("#{고객명}"), java.util.Set.of());
        String infoPrompt = service.buildPrompt(infoRequest, List.of("#{고객명}"), java.util.Set.of());

        assertThat(adPrompt)
                .contains("반드시 '(광고)'로 시작", "080-000-0000", "BENEFIT")
                .contains("제목에는 '(광고)'", "강제로 넣지 마십시오");
        assertThat(infoPrompt)
                .contains("'(광고)' 문구나 수신거부 문구를 강제로 넣지 마십시오")
                .contains("혜택을 과장하거나 광고처럼 보이는");
    }

    private AiSuggestionRequest request(MessageType messageType) {
        AiSuggestionRequest request = new AiSuggestionRequest();
        request.setContextType(AiContextType.MESSAGE_SEND);
        request.setMessageType(messageType);
        request.setChannels(List.of(ChannelType.SMS, ChannelType.KAKAO));
        request.setCustomerTags(List.of("NEW", "패션"));
        request.setDirection("자연스럽게 작성");
        return request;
    }

    private AiSuggestionItem validAd(String title) {
        return new AiSuggestionItem(
                title,
                "(광고) 준비한 혜택을 확인해보세요. 무료수신거부 080-000-0000"
        );
    }

    private AiSuggestionResponse response(AiSuggestionItem... items) {
        return new AiSuggestionResponse(List.of(items));
    }
}
