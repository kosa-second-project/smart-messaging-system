package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class KakaoFriendsResponse {
    private List<KakaoFriendElement> elements;
    private Integer total_count;
    private Integer favorite_count;
    private String before_url;
    private String after_url;
}
