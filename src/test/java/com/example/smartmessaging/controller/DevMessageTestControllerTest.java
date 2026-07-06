package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.DevTestSendRequest;
import com.example.smartmessaging.dto.response.DevTestSendResponse;
import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.dto.vo.UsersVO;
import com.example.smartmessaging.security.CustomUserDetails;
import com.example.smartmessaging.service.DevTestMessageService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevMessageTestControllerTest {

    private final DevTestMessageService devTestMessageService = mock(DevTestMessageService.class);
    private final DevMessageTestController controller = new DevMessageTestController(devTestMessageService);

    @Test
    @DisplayName("개발용 메시지 테스트 요청은 로그인 사용자와 카카오 세션 토큰을 함께 전달한다")
    void sendMessageTest() {
        DevTestSendRequest request = new DevTestSendRequest();
        request.setTitle("테스트 제목");
        request.setContent("#{고객명}님 테스트 본문");
        request.setPurpose("AD");
        request.setLinkUrl("https://example.com");
        request.setLinkPurpose("PURCHASE");

        CustomUserDetails userDetails = new CustomUserDetails(
                UsersVO.builder()
                        .userId(10L)
                        .empNum(2026001)
                        .name("테스터")
                        .pwd("encoded")
                        .isActive(true)
                        .build(),
                List.of("ROLE_USER")
        );
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("kakaoAccessToken")).thenReturn("kakao-token");

        DevTestSendResponse expected = DevTestSendResponse.builder()
                .customerId(100L)
                .customerName("테스터")
                .smsResult(SendResult.success("SMS"))
                .emailResult(SendResult.success("EMAIL"))
                .kakaoResult(SendResult.success("KAKAO"))
                .build();
        when(devTestMessageService.sendToMe(10L, "테스터", "kakao-token", request)).thenReturn(expected);

        ResponseEntity<DevTestSendResponse> response = controller.sendMessageTest(request, userDetails, session);

        assertThat(response.getBody()).isSameAs(expected);
        verify(devTestMessageService).sendToMe(10L, "테스터", "kakao-token", request);
    }
}
