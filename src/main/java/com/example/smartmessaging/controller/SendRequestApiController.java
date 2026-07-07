package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.SendPrepareRequestDTO;
import com.example.smartmessaging.dto.response.SendPrepareResponseDTO;
import com.example.smartmessaging.security.CustomUserDetails;
import com.example.smartmessaging.service.SendPreparationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/send-requests")
@RequiredArgsConstructor
public class SendRequestApiController {

    private final SendPreparationService sendPreparationService;

    @PostMapping
    public ResponseEntity<SendPrepareResponseDTO> prepareSendRequest(
            @Valid @RequestBody SendPrepareRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session
    ) {
        String kakaoAccessToken = (String) session.getAttribute("kakaoAccessToken");
        SendPrepareResponseDTO response = sendPreparationService.prepare(userDetails.getUserId(), request, kakaoAccessToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
