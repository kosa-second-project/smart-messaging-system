package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.response.KakaoFriendElement;
import com.example.smartmessaging.dto.response.KakaoFriendsResponse;
import com.example.smartmessaging.exception.KakaoApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
public class KakaoMessageService {

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KakaoMessageService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 카카오톡 친구 목록 API를 호출하여 친구들의 uuid 목록을 가져옵니다.
     */
    public List<String> getFriendUuids(String accessToken) {
        return getKakaoFriends(accessToken).stream()
                .map(KakaoFriendElement::getUuid)
                .toList();
    }

    /**
     * 카카오톡 친구 목록 전체(상세 정보 포함)를 가져옵니다.
     */
    public List<KakaoFriendElement> getKakaoFriends(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoFriendsResponse> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v1/api/talk/friends",
                    HttpMethod.GET,
                    request,
                    KakaoFriendsResponse.class);

            KakaoFriendsResponse body = response.getBody();
            if (body != null && body.getElements() != null) {
                return body.getElements();
            }
            return List.of();
        } catch (RestClientException e) {
            log.error("Failed to fetch Kakao friends", e);
            throw new KakaoApiException("카카오 친구 목록을 불러오는데 실패했습니다.", e);
        }
    }

    /**
     * 카카오 사용자 프로필 정보를 조회하여 닉네임을 반환합니다.
     */
    public String getMyProfileNickname(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("properties")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> properties = (Map<String, Object>) body.get("properties");
                if (properties != null && properties.containsKey("nickname")) {
                    return (String) properties.get("nickname");
                }
            }
            return "나";
        } catch (Exception e) {
            log.error("Failed to fetch Kakao user profile", e);
            return "나";
        }
    }

    /**
     * 카카오톡 친구들에게 피드(Feed) 템플릿 메시지를 발송합니다.
     */
    public boolean sendFeedMessage(String accessToken, List<String> receiverUuids, String title, String description) {
        return sendFeedMessage(accessToken, receiverUuids, title, description, null);
    }

    public boolean sendFeedMessage(String accessToken, List<String> receiverUuids, String title, String description, String actionUrl) {
        if (receiverUuids == null || receiverUuids.isEmpty()) {
            return false;
        }

        String safeTitle = title != null ? title : "";
        String safeDescription = description != null ? description : "";
        String linkUrl = resolveTemplateLink(actionUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        boolean hasSuccess = false;

        try {
            // 친구 목록을 가져와서 uuid별 닉네임 매핑을 빌드합니다.
            List<KakaoFriendElement> friends = getKakaoFriends(accessToken);
            Map<String, String> uuidToNickname = friends.stream()
                    .filter(f -> f.getUuid() != null && f.getProfileNickname() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            KakaoFriendElement::getUuid,
                            KakaoFriendElement::getProfileNickname,
                            (v1, v2) -> v1
                    ));

            for (String uuid : receiverUuids) {
                try {
                    // 수신자별 이름 치환
                    String nickname = uuidToNickname.getOrDefault(uuid, "고객");
                    String personalizedTitle = safeTitle.replace("#{고객명}", nickname);
                    String personalizedDescription = safeDescription.replace("#{고객명}", nickname);

                    Map<String, Object> templateObject = Map.of(
                            "object_type", "text",
                            "text", personalizedTitle + "\n\n" + personalizedDescription,
                            "button_title", "자세히 보기",
                            "link", Map.of(
                                    "web_url", linkUrl,
                                    "mobile_web_url", linkUrl
                            )
                    );

                    String templateJson = objectMapper.writeValueAsString(templateObject);

                    // 단일 uuid를 포함하는 리스트로 JSON 생성
                    String singleUuidJson = objectMapper.writeValueAsString(List.of(uuid));

                    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                    params.add("receiver_uuids", singleUuidJson);
                    params.add("template_object", templateJson);

                    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

                    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                            "https://kapi.kakao.com/v1/api/talk/friends/message/default/send",
                            HttpMethod.POST,
                            request,
                            new ParameterizedTypeReference<Map<String, Object>>() {});

                    log.info("카카오톡 개별 발송 결과 (uuid: {}): {}", uuid, response.getBody());
                    hasSuccess = true;
                } catch (Exception e) {
                    log.error("Kakao API send message error for uuid: {}", uuid, e);
                }
            }

            if (!hasSuccess) {
                throw new KakaoApiException("모든 대상에게 카카오 메시지 발송을 실패했습니다.");
            }

            return true;

        } catch (Exception e) {
            log.error("Kakao message sending process error", e);
            if (e instanceof KakaoApiException) {
                throw (KakaoApiException) e;
            }
            throw new KakaoApiException("카카오 메시지 발송 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카카오톡 나에게 기본 템플릿(피드) 메시지를 발송합니다.
     */
    public boolean sendMemoMessage(String accessToken, String title, String description) {
        return sendMemoMessage(accessToken, title, description, null);
    }

    public boolean sendMemoMessage(String accessToken, String title, String description, String actionUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        String safeTitle = title != null ? title : "알림";
        String safeDescription = description != null ? description : "";
        String linkUrl = resolveTemplateLink(actionUrl);

        // 내 프로필 닉네임 가져와서 #{고객명} 치환
        String myNickname = getMyProfileNickname(accessToken);
        String personalizedTitle = safeTitle.replace("#{고객명}", myNickname);
        String personalizedDescription = safeDescription.replace("#{고객명}", myNickname);

        Map<String, Object> templateObject = Map.of(
                "object_type", "text",
                "text", personalizedTitle + "\n\n" + personalizedDescription,
                "button_title", "자세히 보기",
                "link", Map.of(
                        "web_url", linkUrl,
                        "mobile_web_url", linkUrl
                )
        );

        try {
            String templateJson = objectMapper.writeValueAsString(templateObject);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("template_object", templateJson);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/api/talk/memo/default/send",
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("카카오톡 나에게 보내기 결과: {}", response.getBody());
            return true;

        } catch (JsonProcessingException e) {
            log.error("JSON parsing error", e);
            throw new KakaoApiException("메시지 포맷 변환에 실패했습니다.", e);
        } catch (RestClientException e) {
            log.error("Kakao API send message error", e);
            throw new KakaoApiException("나에게 메시지 발송에 실패했습니다.", e);
        }
    }

    /**
     * 카카오톡 친구 목록 전체에게 피드(Feed) 템플릿 메시지를 발송합니다.
     */
    public boolean sendFeedMessageToAll(String accessToken, String title, String description) {
        List<KakaoFriendElement> friends = getKakaoFriends(accessToken);
        if (friends == null || friends.isEmpty()) {
            throw new KakaoApiException("발송 가능한 카카오 친구가 없습니다. (메시지 수신 동의 필요)");
        }
        
        List<String> targetUuids = friends.stream()
                .map(KakaoFriendElement::getUuid)
                .toList();
                
        return sendFeedMessage(accessToken, targetUuids, title, description);
    }

    private String resolveTemplateLink(String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank()) {
            return appBaseUrl;
        }
        return actionUrl.trim();
    }
}
