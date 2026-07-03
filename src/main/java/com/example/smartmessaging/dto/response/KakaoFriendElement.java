package com.example.smartmessaging.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoFriendElement {
    @JsonProperty("profile_nickname")
    private String profileNickname;
    
    @JsonProperty("profile_thumbnail_image")
    private String profileThumbnailImage;
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("uuid")
    private String uuid;
    
    @JsonProperty("favorite")
    private Boolean favorite;
}
