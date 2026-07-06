package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.KakaoFeedMessageRequest;
import com.example.smartmessaging.dto.request.SendPrepareRequestDTO;
import com.example.smartmessaging.dto.response.CostEstimationResponseDTO;
import com.example.smartmessaging.dto.response.SendPrepareResponseDTO;
import com.example.smartmessaging.security.CustomUserDetails;
import com.example.smartmessaging.service.KakaoMessageService;
import com.example.smartmessaging.service.SendPreparationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/send")
@RequiredArgsConstructor
public class SendApiController {

    private final KakaoMessageService kakaoMessageService;
    private final SendPreparationService sendPreparationService;

    private String getKakaoAccessToken(HttpSession session) {
        String token = (String) session.getAttribute("kakaoAccessToken");
        return (token != null && !token.isEmpty()) ? token : null;
    }

    private ResponseEntity<?> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "카카오 로그인이 필요합니다."));
    }

    @PostMapping("/estimate-cost")
    public ResponseEntity<CostEstimationResponseDTO> estimateCost(
            @Valid @RequestBody SendPrepareRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(sendPreparationService.estimate(userDetails.getUserId(), request));
    }

    @PostMapping("/campaigns")
    public ResponseEntity<SendPrepareResponseDTO> queueCampaign(
            @Valid @RequestBody SendPrepareRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.accepted().body(sendPreparationService.queueCampaign(userDetails.getUserId(), request));
    }

    @GetMapping("/kakao/friends")
    public ResponseEntity<?> getKakaoFriends(HttpSession session) {
        String kakaoAccessToken = getKakaoAccessToken(session);
        if (kakaoAccessToken == null) return unauthorizedResponse();

        List<com.example.smartmessaging.dto.response.KakaoFriendElement> friends = kakaoMessageService.getKakaoFriends(kakaoAccessToken);
        return ResponseEntity.ok(friends);
    }

    @PostMapping("/kakao")
    public ResponseEntity<?> sendKakaoMessage(@RequestBody KakaoFeedMessageRequest request, HttpSession session) {
        String kakaoAccessToken = getKakaoAccessToken(session);
        if (kakaoAccessToken == null) return unauthorizedResponse();

        List<String> targetUuids = request.getTargetUuids();
        
        if (targetUuids == null || targetUuids.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("message", "수신 대상 친구(UUID)를 선택해주세요."));
        }

        // 카카오톡 메시지 전송
        boolean isSuccess = kakaoMessageService.sendFeedMessage(kakaoAccessToken, targetUuids, request.getTitle(), request.getDescription());

        if (isSuccess) {
            return ResponseEntity.ok(Map.of("message", "선택한 카카오 친구들에게 메시지를 성공적으로 발송했습니다."));
        } else {
            return ResponseEntity.status(500).body(Map.of("message", "카카오톡 메시지 발송에 실패했습니다."));
        }
    }

    @PostMapping("/kakao/me")
    public ResponseEntity<?> sendKakaoMessageToMe(@RequestBody KakaoFeedMessageRequest request, HttpSession session) {
        String kakaoAccessToken = getKakaoAccessToken(session);
        if (kakaoAccessToken == null) return unauthorizedResponse();

        boolean isSuccess = kakaoMessageService.sendMemoMessage(kakaoAccessToken, request.getTitle(), request.getDescription());
        
        return ResponseEntity.ok(Map.of("success", isSuccess, "message", isSuccess ? "발송 성공" : "발송 실패"));
    }
    @PostMapping("/kakao/all")
    public ResponseEntity<?> sendKakaoMessageToAll(@RequestBody KakaoFeedMessageRequest request, HttpSession session) {
        String kakaoAccessToken = getKakaoAccessToken(session);
        if (kakaoAccessToken == null) return unauthorizedResponse();

        boolean isSuccess = kakaoMessageService.sendFeedMessageToAll(kakaoAccessToken, request.getTitle(), request.getDescription());

        if (isSuccess) {
            return ResponseEntity.ok(Map.of("message", "모든 카카오 친구에게 메시지를 성공적으로 발송했습니다."));
        } else {
            return ResponseEntity.status(500).body(Map.of("message", "카카오톡 전체 메시지 발송에 실패했습니다."));
        }
    }
}

