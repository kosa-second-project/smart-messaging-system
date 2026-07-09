package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.client.GeminiSuggestionClient;
import com.example.smartmessaging.ai.dto.request.AiSuggestionRequestDTO;
import com.example.smartmessaging.ai.dto.response.AiSuggestionItemResponseDTO;
import com.example.smartmessaging.ai.dto.response.AiSuggestionResponseDTO;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.TemplateCategory;
import com.example.smartmessaging.ai.rag.service.RagPromptContextService;
import com.example.smartmessaging.ai.rag.service.RagPromptContextService.RagPromptContext;
import com.example.smartmessaging.ai.rag.service.RagPromptContextService.RagReferenceLog;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
        AiSuggestionItemResponseDTO first = validAd("첫 번째");
        AiSuggestionItemResponseDTO rejected = new AiSuggestionItemResponseDTO("제외", "문의 test@example.com");
        AiSuggestionItemResponseDTO second = validAd("두 번째");
        AiSuggestionItemResponseDTO third = validAd("세 번째");
        AiSuggestionItemResponseDTO fourth = validAd("네 번째");
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(first, rejected, second, third, fourth)
        );

        AiSuggestionResponseDTO result = service.suggest(request(MessageType.AD));

        assertThat(result.suggestions())
                .extracting(AiSuggestionItemResponseDTO::title)
                .containsExactly("첫 번째", "두 번째", "세 번째");
        verify(geminiSuggestionClient, times(1)).generate(anyString());
    }

    @Test
    void 통과_후보가_1개이면_재시도하지_않고_반환한다() {
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(
                        validAd("통과"),
                        new AiSuggestionItemResponseDTO("제외", "문의 test@example.com")
                )
        );

        AiSuggestionResponseDTO result = service.suggest(request(MessageType.AD));

        assertThat(result.suggestions()).hasSize(1);
        verify(geminiSuggestionClient, times(1)).generate(anyString());
    }

    @Test
    void 통과_후보가_0개이면_실패_ruleId를_반영해_재시도한다() {
        when(geminiSuggestionClient.generate(anyString()))
                .thenReturn(response(new AiSuggestionItemResponseDTO("제외", "문의 test@example.com")))
                .thenReturn(response(validAd("재시도 통과")));

        AiSuggestionResponseDTO result = service.suggest(request(MessageType.AD));

        assertThat(result.suggestions()).hasSize(1);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiSuggestionClient, times(2)).generate(promptCaptor.capture());
        assertThat(promptCaptor.getAllValues().get(1)).isNotBlank();
    }

    @Test
    void null_suggestions_응답은_빈_후보로_보고_재시도한다() {
        when(geminiSuggestionClient.generate(anyString()))
                .thenReturn(new AiSuggestionResponseDTO(null))
                .thenReturn(response(validAd("재시도 통과")));

        AiSuggestionResponseDTO result = service.suggest(request(MessageType.AD));

        assertThat(result.suggestions()).hasSize(1);
        verify(geminiSuggestionClient, times(2)).generate(anyString());
    }

    @Test
    void 총_3회_모두_통과_후보가_없으면_422_예외을_발생시킨다() {
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(new AiSuggestionItemResponseDTO("제외", "문의 test@example.com"))
        );

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.suggest(request(MessageType.AD)))
                .withMessage("조건을 만족하는 추천 문구를 생성하지 못했습니다.")
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.AI_SUGGESTION_NO_VALID_CANDIDATE));
        verify(geminiSuggestionClient, times(2)).generate(anyString());
    }

    @Test
    void availableVariables가_없으면_고객명만_프롬프트와_검증에_사용한다() {
        AiSuggestionRequestDTO request = request(MessageType.INFO);
        request = withAvailableVariables(request, null);
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(new AiSuggestionItemResponseDTO("배송 안내", "#{고객명}님, 주문 상품이 출고되었습니다."))
        );

        AiSuggestionResponseDTO result = service.suggest(request);

        assertThat(result.suggestions()).hasSize(1);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiSuggestionClient).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("허용 변수: [#{고객명}]")
                .doesNotContain("#{주문번호}", "#{쿠폰명}");
    }

    @Test
    void 주문번호와_쿠폰명으로_생성된_후보는_제외한다() {
        AiSuggestionRequestDTO request = request(MessageType.INFO);
        when(geminiSuggestionClient.generate(anyString())).thenReturn(response(
                new AiSuggestionItemResponseDTO("주문 안내", "#{주문번호} 주문을 확인해 주세요."),
                new AiSuggestionItemResponseDTO("쿠폰 안내", "#{쿠폰명}을 확인해 주세요."),
                new AiSuggestionItemResponseDTO("배송 안내", "#{고객명}님, 배송이 시작되었습니다.")
        ));

        AiSuggestionResponseDTO result = service.suggest(request);

        assertThat(result.suggestions())
                .extracting(AiSuggestionItemResponseDTO::title)
                .containsExactly("배송 안내");
        verify(geminiSuggestionClient, times(1)).generate(anyString());
    }

    @Test
    void customerTags가_없어도_추천_문구를_생성한다() {
        AiSuggestionRequestDTO request = request(MessageType.INFO);
        request = withCustomerTags(request, null);
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(new AiSuggestionItemResponseDTO("배송 안내", "#{고객명}님, 주문 상품이 출고되었습니다."))
        );

        AiSuggestionResponseDTO result = service.suggest(request);

        assertThat(result.suggestions()).hasSize(1);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiSuggestionClient).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("고객 태그: []");
    }

    @Test
    void 사용자가_지정한_변수만_허용한다() {
        AiSuggestionRequestDTO request = request(MessageType.INFO);
        request = withAvailableVariables(request, List.of("#{고객명}"));
        when(geminiSuggestionClient.generate(anyString())).thenReturn(
                response(new AiSuggestionItemResponseDTO("안내", "#{고객명}님, 배송이 시작되었습니다."))
        );

        AiSuggestionResponseDTO result = service.suggest(request);

        assertThat(result.suggestions()).hasSize(1);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiSuggestionClient).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("허용 변수: [#{고객명}]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"#{주문번호}", "#{쿠폰명}", "#{만료일}"})
    void AI_추천이_지원하지_않는_변수는_400으로_거부한다(String variable) {
        AiSuggestionRequestDTO request = request(MessageType.INFO);
        AiSuggestionRequestDTO invalidRequest = withAvailableVariables(request, List.of(variable));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.suggest(invalidRequest))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
                );
        verify(geminiSuggestionClient, never()).generate(anyString());
    }

    @Test
    void AD와_INFO_규칙과_템플릿_카테고리를_프롬프트에_반영한다() {
        AiSuggestionRequestDTO adRequest = request(MessageType.AD);
        adRequest = new AiSuggestionRequestDTO(AiContextType.TEMPLATE_CREATE, adRequest.messageType(), adRequest.channels(), adRequest.customerTags(), adRequest.direction(), TemplateCategory.BENEFIT, adRequest.availableVariables());
        AiSuggestionRequestDTO infoRequest = request(MessageType.INFO);

        String adPrompt = service.buildPrompt(adRequest, List.of("#{고객명}"), java.util.Set.of());
        String infoPrompt = service.buildPrompt(infoRequest, List.of("#{고객명}"), java.util.Set.of());

        assertThat(adPrompt).contains("BENEFIT");
        assertThat(infoPrompt).isNotBlank();
    }

    @Test
    void SMS와_LMS가_함께_있으면_SMS_길이_규칙을_우선하고_부가_채널_규칙은_유지한다() {
        AiSuggestionRequestDTO request = request(MessageType.INFO);
        request = withChannels(request, List.of(ChannelType.SMS, ChannelType.LMS, ChannelType.KAKAO));

        String prompt = service.buildPrompt(request, List.of("#{고객명}"), java.util.Set.of());

        assertThat(prompt)
                .contains("SMS가 포함되어 있으므로 본문을 짧고 간결하게")
                .contains("카카오 메시지에 어울리는 친근하지만 과하지 않은 톤")
                .contains("가장 제약이 큰 채널을 기준으로")
                .doesNotContain("LMS에 맞게 SMS보다 조금 자세하되");
    }

    @Test
    void RAG_context_isIncludedInGeminiPromptAndBuiltOncePerRequest() {
        RagPromptContextService ragPromptContextService = mock(RagPromptContextService.class);
        AiSuggestionService serviceWithRag = new AiSuggestionService(
                geminiSuggestionClient,
                new RuleValidationService(),
                ragPromptContextService
        );
        AiSuggestionRequestDTO request = request(MessageType.AD);
        // 추천 재시도마다 Qdrant를 다시 치지 않고, 첫 검색 결과를 프롬프트에 계속 재사용하는지 확인한다.
        when(ragPromptContextService.buildSuggestionPromptContext(request))
                .thenReturn(new RagPromptContext(
                        "\n[RAG Reference Materials]\n- tone reference only\n",
                        List.of(new RagReferenceLog(
                                "hmall-campaign-001",
                                0.82,
                                "campaign_copy",
                                List.of("coupon"),
                                List.of("tone_reference"),
                                List.of("100%"),
                                "Hmall coupon reference preview"
                        ))
                ));
        when(geminiSuggestionClient.generate(anyString()))
                .thenReturn(response(new AiSuggestionItemResponseDTO("excluded", "contact test@example.com")))
                .thenReturn(response(validAd("retry passed")));

        AiSuggestionResponseDTO result = serviceWithRag.suggest(request);

        assertThat(result.suggestions()).hasSize(1);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiSuggestionClient, times(2)).generate(promptCaptor.capture());
        assertThat(promptCaptor.getAllValues().get(0)).contains("[RAG Reference Materials]");
        assertThat(promptCaptor.getAllValues().get(1)).contains("[RAG Reference Materials]");
        verify(ragPromptContextService, times(1)).buildSuggestionPromptContext(request);
    }

    private AiSuggestionRequestDTO request(MessageType messageType) {
        return new AiSuggestionRequestDTO(
                AiContextType.MESSAGE_SEND,
                messageType,
                List.of(ChannelType.SMS, ChannelType.KAKAO),
                List.of("NEW", "패션"),
                "자연스럽게 작성",
                null,
                null
        );
    }

    private AiSuggestionRequestDTO withAvailableVariables(AiSuggestionRequestDTO request, List<String> availableVariables) {
        return new AiSuggestionRequestDTO(
                request.contextType(),
                request.messageType(),
                request.channels(),
                request.customerTags(),
                request.direction(),
                request.category(),
                availableVariables
        );
    }

    private AiSuggestionRequestDTO withCustomerTags(AiSuggestionRequestDTO request, List<String> customerTags) {
        return new AiSuggestionRequestDTO(
                request.contextType(),
                request.messageType(),
                request.channels(),
                customerTags,
                request.direction(),
                request.category(),
                request.availableVariables()
        );
    }

    private AiSuggestionRequestDTO withChannels(AiSuggestionRequestDTO request, List<ChannelType> channels) {
        return new AiSuggestionRequestDTO(
                request.contextType(),
                request.messageType(),
                channels,
                request.customerTags(),
                request.direction(),
                request.category(),
                request.availableVariables()
        );
    }

    private AiSuggestionItemResponseDTO validAd(String title) {
        return new AiSuggestionItemResponseDTO(
                title,
                "준비한 혜택을 확인해보세요."
        );
    }

    private AiSuggestionResponseDTO response(AiSuggestionItemResponseDTO... items) {
        return new AiSuggestionResponseDTO(List.of(items));
    }
}
