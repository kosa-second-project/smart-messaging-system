package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.DevTestSendRequest;
import com.example.smartmessaging.dto.response.DevTestSendResponse;
import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.dto.vo.UsersVO;
import com.example.smartmessaging.security.CustomUserDetails;
import com.example.smartmessaging.service.DevTestMessageService;
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
    void sendMessageTestDelegatesToService() {
        DevTestSendRequest request = new DevTestSendRequest();
        request.setTitle("test title");
        request.setContent("test content");
        request.setPurpose("AD");
        request.setLinkUrl("https://example.com");
        request.setLinkPurpose("PURCHASE");

        CustomUserDetails userDetails = new CustomUserDetails(
                UsersVO.builder()
                        .userId(10L)
                        .empNum(2026001)
                        .name("Tester")
                        .pwd("encoded")
                        .isActive(true)
                        .build(),
                List.of("ROLE_USER")
        );

        DevTestSendResponse expected = DevTestSendResponse.builder()
                .customerId(100L)
                .customerName("Tester")
                .smsResult(SendResult.success("SMS"))
                .emailResult(SendResult.success("EMAIL"))
                .build();
        when(devTestMessageService.sendToMe(10L, "Tester", request)).thenReturn(expected);

        ResponseEntity<DevTestSendResponse> response = controller.sendMessageTest(request, userDetails);

        assertThat(response.getBody()).isSameAs(expected);
        verify(devTestMessageService).sendToMe(10L, "Tester", request);
    }
}