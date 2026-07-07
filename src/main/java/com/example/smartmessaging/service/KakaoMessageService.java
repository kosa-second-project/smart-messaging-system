package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.response.KakaoFriendElement;
import java.util.List;

/**
 * 카카오 메시지 발송 서비스 인터페이스
 */
public interface KakaoMessageService {

    List<String> getFriendUuids(String accessToken);

    List<KakaoFriendElement> getKakaoFriends(String accessToken);


    boolean sendFeedMessage(String accessToken, List<String> receiverUuids, String title, String description);

    boolean sendFeedMessage(String accessToken, List<String> receiverUuids, String title, String description, String buttonName, String actionUrl);


    boolean sendFeedMessageToAll(String accessToken, String title, String description);

    boolean sendFeedMessageToAll(String accessToken, String title, String description, String buttonName, String actionUrl);
}
