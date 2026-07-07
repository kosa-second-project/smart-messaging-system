package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ShortUrlTargetVO;
import com.example.smartmessaging.dto.vo.ShortUrlVO;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.example.smartmessaging.service.repository.ShortUrlMapper;
import com.example.smartmessaging.service.repository.UnsubscribeMapper;
import com.example.smartmessaging.service.ShortUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 단축 URL 및 수신거부 처리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
public class ShortUrlServiceImpl implements ShortUrlService {

    private static final String CODE_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_GENERATE_ATTEMPTS = 5;
    private static final Long SYSTEM_ACTOR_ID = 1L;

    private final ShortUrlMapper shortUrlMapper;
    private final UnsubscribeMapper unsubscribeMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${app.short-url-base-url:${app.base-url:http://localhost:8080}}")
    private String shortUrlBaseUrl;

    @Override
    @Transactional
    public String createTrackedUrl(Long sendTargetId, String originalUrl, ShortUrlPurpose purpose) {
        ShortUrlPurpose resolvedPurpose = purpose == null ? ShortUrlPurpose.CLICK : purpose;
        String resolvedOriginalUrl = resolveOriginalUrl(originalUrl, resolvedPurpose);
        validateOriginalUrl(resolvedOriginalUrl, resolvedPurpose);

        String code = createShortUrlRow(sendTargetId, resolvedOriginalUrl, resolvedPurpose);
        return buildShortUrl(code, resolvedPurpose);
    }

    @Override
    public String getTrackedUrl(Long sendTargetId, ShortUrlPurpose purpose) {
        if (sendTargetId == null) {
            return null;
        }
        ShortUrlPurpose resolvedPurpose = purpose == null ? ShortUrlPurpose.CLICK : purpose;
        ShortUrlVO existing = shortUrlMapper.findBySendTargetIdAndPurpose(sendTargetId, resolvedPurpose.name());
        if (existing == null || existing.getId() == null || existing.getId().isBlank()) {
            return null;
        }
        return buildShortUrl(existing.getId(), resolvedPurpose);
    }

    @Override
    public ShortUrlVO getExisting(String code) {
        ShortUrlVO shortUrl = shortUrlMapper.findById(code);
        if (shortUrl == null) {
            throw new BusinessException("유효하지 않은 링크입니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        return shortUrl;
    }

    @Override
    @Transactional
    public String markClickAndResolveRedirect(String code) {
        ShortUrlTargetVO target = shortUrlMapper.findClickTargetById(code);
        if (target == null) {
            throw new BusinessException("유효하지 않은 링크입니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        ShortUrlPurpose purpose = ShortUrlPurpose.from(target.getPurpose());
        if (!purpose.countsAsClickRate()) {
            throw new BusinessException("클릭 추적 링크가 아닙니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        boolean firstClick = shortUrlMapper.markFirstClicked(code) > 0;
        if (firstClick) {
            recordClickStats(target);
        }
        return appendTrackingParameters(target.getOriginalUrl(), target);
    }

    @Override
    public ShortUrlTargetVO getUnsubscribeTarget(String code) {
        ShortUrlVO shortUrl = getExisting(code);
        if (ShortUrlPurpose.from(shortUrl.getPurpose()) != ShortUrlPurpose.UNSUBSCRIBE) {
            throw new BusinessException("수신거부 링크가 아닙니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        ShortUrlTargetVO target = unsubscribeMapper.findTargetBySendTargetId(shortUrl.getSendTargetId());
        if (target == null) {
            throw new BusinessException("수신거부 대상을 찾을 수 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        return target;
    }

    @Override
    @Transactional
    public ShortUrlTargetVO unsubscribe(String code) {
        ShortUrlTargetVO target = getUnsubscribeTarget(code);
        shortUrlMapper.markClicked(code);
        unsubscribeMapper.blockCustomerAds(target.getCustomerId(), SYSTEM_ACTOR_ID);
        unsubscribeMapper.revokeSmsConsent(target.getCustomerId(), SYSTEM_ACTOR_ID);
        unsubscribeMapper.insertRejectHistoryIfAbsent(target.getCustomerId(), SYSTEM_ACTOR_ID);
        return target;
    }

    private String createShortUrlRow(Long sendTargetId, String originalUrl, ShortUrlPurpose purpose) {
        for (int i = 0; i < MAX_GENERATE_ATTEMPTS; i++) {
            String code = generateCode();
            ShortUrlVO row = new ShortUrlVO();
            row.setId(code);
            row.setOriginalUrl(originalUrl);
            row.setSendTargetId(sendTargetId);
            row.setPurpose(purpose.name());
            row.setIsConverted(purpose == ShortUrlPurpose.PURCHASE ? Boolean.FALSE : null);
            row.setCreatedBy(SYSTEM_ACTOR_ID);
            row.setUpdatedBy(SYSTEM_ACTOR_ID);
            try {
                shortUrlMapper.insertShortUrl(row);
                recordClickTargetStats(sendTargetId, purpose);
                return code;
            } catch (DuplicateKeyException duplicateKeyException) {
                if (i == MAX_GENERATE_ATTEMPTS - 1) {
                    throw duplicateKeyException;
                }
            }
        }
        throw new BusinessException("추적 링크 생성에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private String buildShortUrl(String code, ShortUrlPurpose purpose) {
        String path = purpose == ShortUrlPurpose.UNSUBSCRIBE ? "/u/" : "/r/";
        return shortUrlBaseUrl + path + code;
    }

    private void recordClickTargetStats(Long sendTargetId, ShortUrlPurpose purpose) {
        if (!purpose.countsAsClickRate()) {
            return;
        }
        ShortUrlTargetVO target = shortUrlMapper.findTargetBySendTargetId(sendTargetId);
        if (target == null || target.getChannelId() == null) {
            return;
        }
        shortUrlMapper.incrementChannelClickTargetCount(target.getChannelId(), SYSTEM_ACTOR_ID);
    }

    private void recordClickStats(ShortUrlTargetVO target) {
        if (target.getChannelId() == null) {
            return;
        }
        shortUrlMapper.incrementChannelClickCount(target.getChannelId(), SYSTEM_ACTOR_ID);
        shortUrlMapper.incrementHourlyClickCount(target.getChannelId(), LocalDateTime.now().getHour(), SYSTEM_ACTOR_ID);
    }

    private String appendTrackingParameters(String originalUrl, ShortUrlTargetVO target) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(originalUrl);
        if (target.getUserUuid() != null && !target.getUserUuid().isBlank()) {
            builder.queryParam("userUuid", target.getUserUuid());
        }
        if (target.getChannelId() != null) {
            builder.queryParam("channelId", target.getChannelId());
        }
        return builder.build().toUriString();
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private void validateOriginalUrl(String originalUrl, ShortUrlPurpose purpose) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new BusinessException("링크 URL은 필수입니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        String scheme;
        try {
            scheme = UriComponentsBuilder.fromUriString(originalUrl.trim()).build().getScheme();
        } catch (IllegalArgumentException e) {
            throw new BusinessException("링크 URL 형식이 올바르지 않습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        String normalizedScheme = scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
            throw new BusinessException("링크 URL은 http 또는 https만 사용할 수 있습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String resolveOriginalUrl(String originalUrl, ShortUrlPurpose purpose) {
        if (purpose == ShortUrlPurpose.UNSUBSCRIBE && (originalUrl == null || originalUrl.isBlank())) {
            return appBaseUrl + "/u/complete";
        }
        return originalUrl;
    }
}
