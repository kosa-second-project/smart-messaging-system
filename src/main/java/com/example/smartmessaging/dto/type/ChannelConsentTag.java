package com.example.smartmessaging.dto.type;

import lombok.Getter;

@Getter
public enum ChannelConsentTag {
    EMAIL("EMAIL", "이메일 동의"),
    SMS("SMS", "sms 동의"),
    LMS("LMS", "sms 동의"),
    KAKAO("KAKAO", "카카오 동의");

    private final String channelType;
    private final String tagName;

    ChannelConsentTag(String channelType, String tagName) {
        this.channelType = channelType;
        this.tagName = tagName;
    }

    public static String getTagNameByChannel(String channelType) {
        for (ChannelConsentTag c : values()) {
            if (c.getChannelType().equalsIgnoreCase(channelType)) {
                return c.getTagName();
            }
        }
        return null;
    }
}
