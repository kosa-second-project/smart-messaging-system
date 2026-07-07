package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ShortUrlTargetVO;
import com.example.smartmessaging.dto.vo.ShortUrlVO;

/**
 * 단축 URL 및 수신거부 처리 서비스 인터페이스
 */
public interface ShortUrlService {

    String createTrackedUrl(Long sendTargetId, String originalUrl, ShortUrlPurpose purpose);

    String getTrackedUrl(Long sendTargetId, ShortUrlPurpose purpose);

    ShortUrlVO getExisting(String code);

    String markClickAndResolveRedirect(String code);

    ShortUrlTargetVO getUnsubscribeTarget(String code);

    ShortUrlTargetVO unsubscribe(String code);
}
