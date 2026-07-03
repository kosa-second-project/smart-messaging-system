package com.example.smartmessaging.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;

@Controller
public class KakaoOAuthController {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    /**
     * 카카오 로그인 연동 페이지로 리다이렉트
     */
    @GetMapping("/kakao/login")
    public String kakaoLogin() {
        String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + redirectUri + "&response_type=code"
                + "&scope=friends,talk_message"; // 친구 목록 + 메시지 발송 권한 요청
        return "redirect:" + kakaoAuthUrl;
    }

    /**
     * 카카오 로그인 콜백 - Access Token 발급 및 세션 저장
     */
    @GetMapping("/kakao/login/callback")
    public String kakaoCallback(@RequestParam String code, HttpSession session) {
        RestTemplate restTemplate = new RestTemplate();

        // HttpHeader 오브젝트 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HttpBody 오브젝트 생성
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isEmpty()) {
            params.add("client_secret", clientSecret);
        }
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        // HttpHeader와 HttpBody를 하나의 오브젝트에 담기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);

        try {
            // 카카오에 토큰 발급 요청
            ResponseEntity<com.example.smartmessaging.dto.response.KakaoTokenResponse> response = restTemplate.exchange(
                    "https://kauth.kakao.com/oauth/token",
                    HttpMethod.POST,
                    kakaoTokenRequest,
                    com.example.smartmessaging.dto.response.KakaoTokenResponse.class
            );

            com.example.smartmessaging.dto.response.KakaoTokenResponse body = response.getBody();
            if (body != null && body.getAccessToken() != null) {
                String accessToken = body.getAccessToken();
                // 토큰을 세션에 저장
                session.setAttribute("kakaoAccessToken", accessToken);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 실패 시 처리 (로그 남기기 등)
        }

        // 로그인 성공 후 발송 페이지로 이동
        return "redirect:/send/message";
    }
}
