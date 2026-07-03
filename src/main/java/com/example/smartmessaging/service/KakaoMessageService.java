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

@Slf4j
@Service
public class KakaoMessageService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * 카카오톡 친구들에게 피드(Feed) 템플릿 메시지를 발송합니다.
     */
    public boolean sendFeedMessage(String accessToken, List<String> receiverUuids, String title, String description) {
        if (receiverUuids == null || receiverUuids.isEmpty()) {
            return false;
        }

        String safeTitle = title != null ? title : "";
        String safeDescription = description != null ? description : "";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, Object> templateObject = Map.of(
                "object_type", "text",
                "text", safeTitle + "\n\n" + safeDescription,
                "link", Map.of(
                        "web_url", "http://localhost:8080",
                        "mobile_web_url", "http://localhost:8080"
                )
        );

        boolean hasSuccess = false;

        try {
            String templateJson = objectMapper.writeValueAsString(templateObject);

            for (String uuid : receiverUuids) {
                try {
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
                } catch (RestClientException e) {
                    log.error("Kakao API send message error for uuid: {}", uuid, e);
                    // 실패한 건이 있어도 다음 uuid 발송을 위해 계속 진행합니다.
                }
            }

            if (!hasSuccess) {
                throw new KakaoApiException("모든 대상에게 카카오 메시지 발송을 실패했습니다.");
            }

            return true;

        } catch (JsonProcessingException e) {
            log.error("JSON parsing error", e);
            throw new KakaoApiException("메시지 포맷 변환에 실패했습니다.", e);
        }
    }

    /**
     * 카카오톡 나에게 기본 템플릿(피드) 메시지를 발송합니다.
     */
    public boolean sendMemoMessage(String accessToken, String title, String description) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        String safeTitle = title != null ? title : "알림";
        String safeDescription = description != null ? description : "";

        Map<String, Object> templateObject = Map.of(
                "object_type", "text",
                "text", safeTitle + "\n\n" + safeDescription,
                "link", Map.of(
                        "web_url", "http://localhost:8080",
                        "mobile_web_url", "http://localhost:8080"
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
}
