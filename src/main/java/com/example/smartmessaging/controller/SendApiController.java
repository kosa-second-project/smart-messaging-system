package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.KakaoFeedMessageRequest;
import com.example.smartmessaging.service.KakaoMessageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.smartmessaging.security.CustomUserDetails;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/send")
@RequiredArgsConstructor
public class SendApiController {

    private final KakaoMessageService kakaoMessageService;
    private final com.example.smartmessaging.service.SendQueueService sendQueueService;

    private String getKakaoAccessToken(HttpSession session) {
        String token = (String) session.getAttribute("kakaoAccessToken");
        return (token != null && !token.isEmpty()) ? token : null;
    }

    private ResponseEntity<?> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "카카오 로그인이 필요합니다."));
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

        try {
            // 정식 발송 파이프라인: 무조건 대기열 큐(RabbitMQ)에 비동기로 일괄 적재
            sendQueueService.queueKakaoMessages(1L, "admin", request);
            
            return ResponseEntity.ok(Map.of("message", "발송 요청 " + targetUuids.size() + "건이 대기열 큐에 등록되어 비동기 발송을 개시했습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "대기열 큐 등록 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/kakao/me")
    public ResponseEntity<?> sendKakaoMessageToMe(@RequestBody KakaoFeedMessageRequest request, HttpSession session) {
        String kakaoAccessToken = getKakaoAccessToken(session);
        if (kakaoAccessToken == null) return unauthorizedResponse();

        boolean isSuccess = kakaoMessageService.sendMemoMessage(
                kakaoAccessToken,
                request.getTitle(),
                request.getDescription(),
                request.getLinkButtonName(),
                request.getLinkUrl()
        );
        
        return ResponseEntity.ok(Map.of("success", isSuccess, "message", isSuccess ? "발송 성공" : "발송 실패"));
    }
    
    @PostMapping("/kakao/all")
    public ResponseEntity<?> sendKakaoMessageToAll(@RequestBody KakaoFeedMessageRequest request, HttpSession session) {
        String kakaoAccessToken = getKakaoAccessToken(session);
        if (kakaoAccessToken == null) return unauthorizedResponse();

        boolean isSuccess = kakaoMessageService.sendFeedMessageToAll(
                kakaoAccessToken,
                request.getTitle(),
                request.getDescription(),
                request.getLinkButtonName(),
                request.getLinkUrl()
        );

        if (isSuccess) {
            return ResponseEntity.ok(Map.of("message", "모든 카카오 친구에게 메시지를 성공적으로 발송했습니다."));
        } else {
            return ResponseEntity.status(500).body(Map.of("message", "카카오톡 전체 메시지 발송에 실패했습니다."));
        }
    }

    /**
     * DB 수신자 대상의 통합 대량 캠페인 발송 API (비동기 큐 전송)
     */
    @PostMapping("/campaign")
    public ResponseEntity<?> sendCampaign(
            @RequestBody com.example.smartmessaging.dto.request.MessageSendRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long userId = (userDetails != null) ? userDetails.getUserId() : 1L; // 비인증시 가상 어드민 ID 1 부여
            sendQueueService.queueCampaignMessages(userId, request.getDraftId(), request);
            return ResponseEntity.ok(Map.of("message", "캠페인 대량 발송 작업이 성공적으로 비동기 대기열 큐에 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "대기열 큐 등록 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
