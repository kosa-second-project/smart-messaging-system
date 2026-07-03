package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.KakaoFeedMessageRequest;
import com.example.smartmessaging.service.KakaoMessageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/send")
@RequiredArgsConstructor
public class SendApiController {

    private final KakaoMessageService kakaoMessageService;

    @GetMapping("/kakao/friends")
    public ResponseEntity<?> getKakaoFriends(HttpSession session) {
        String kakaoAccessToken = (String) session.getAttribute("kakaoAccessToken");
        
        if (kakaoAccessToken == null || kakaoAccessToken.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "카카오 로그인이 필요합니다."));
        }

        List<com.example.smartmessaging.dto.response.KakaoFriendElement> friends = kakaoMessageService.getKakaoFriends(kakaoAccessToken);
        return ResponseEntity.ok(friends);
    }

    @PostMapping("/kakao")
    public ResponseEntity<?> sendKakaoMessage(@RequestBody KakaoFeedMessageRequest request, HttpSession session) {
        String kakaoAccessToken = (String) session.getAttribute("kakaoAccessToken");
        
        if (kakaoAccessToken == null || kakaoAccessToken.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "카카오 로그인이 필요합니다."));
        }

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
        String kakaoAccessToken = (String) session.getAttribute("kakaoAccessToken");
        
        if (kakaoAccessToken == null || kakaoAccessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Kakao access token not found. Please login again."));
        }

        boolean isSuccess = kakaoMessageService.sendMemoMessage(kakaoAccessToken, request.getTitle(), request.getDescription());
        
        return ResponseEntity.ok(Map.of("success", isSuccess, "message", isSuccess ? "발송 성공" : "발송 실패"));
    }
    @PostMapping("/kakao/all")
    public ResponseEntity<?> sendKakaoMessageToAll(@RequestBody KakaoFeedMessageRequest request, HttpSession session) {
        String kakaoAccessToken = (String) session.getAttribute("kakaoAccessToken");
        
        if (kakaoAccessToken == null || kakaoAccessToken.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "카카오 로그인이 필요합니다."));
        }

        boolean isSuccess = kakaoMessageService.sendFeedMessageToAll(kakaoAccessToken, request.getTitle(), request.getDescription());

        if (isSuccess) {
            return ResponseEntity.ok(Map.of("message", "모든 카카오 친구에게 메시지를 성공적으로 발송했습니다."));
        } else {
            return ResponseEntity.status(500).body(Map.of("message", "카카오톡 전체 메시지 발송에 실패했습니다."));
        }
    }
}

