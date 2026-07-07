package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.DevTestSendRequest;
import com.example.smartmessaging.dto.response.DevTestSendResponse;
import com.example.smartmessaging.security.CustomUserDetails;
import com.example.smartmessaging.service.DevTestMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevMessageTestController {

    private final DevTestMessageService devTestMessageService;

    @PostMapping("/message-test")
    public ResponseEntity<DevTestSendResponse> sendMessageTest(
            @Valid @RequestBody DevTestSendRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DevTestSendResponse response = devTestMessageService.sendToMe(
                userDetails.getUserId(),
                userDetails.getName(),
                request
        );
        return ResponseEntity.ok(response);
    }
}
