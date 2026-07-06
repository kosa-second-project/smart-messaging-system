package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ShortUrlTargetVO;
import com.example.smartmessaging.dto.vo.ShortUrlVO;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.example.smartmessaging.mapper.ShortUrlMapper;
import com.example.smartmessaging.mapper.UnsubscribeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private static final String CODE_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 18;
    private static final int MAX_GENERATE_ATTEMPTS = 5;
    private static final Long SYSTEM_ACTOR_ID = 1L;

    private final ShortUrlMapper shortUrlMapper;
    private final UnsubscribeMapper unsubscribeMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Transactional
    public String createTrackedUrl(Long sendTargetId, String originalUrl, ShortUrlPurpose purpose) {
        ShortUrlPurpose resolvedPurpose = purpose == null ? ShortUrlPurpose.CLICK : purpose;
        String resolvedOriginalUrl = resolveOriginalUrl(originalUrl, resolvedPurpose);
        validateOriginalUrl(resolvedOriginalUrl, resolvedPurpose);

        String code = createShortUrlRow(sendTargetId, resolvedOriginalUrl, resolvedPurpose);
        String path = resolvedPurpose == ShortUrlPurpose.UNSUBSCRIBE ? "/u/" : "/r/";
        return appBaseUrl + path + code;
    }

    public ShortUrlVO getExisting(String code) {
        ShortUrlVO shortUrl = shortUrlMapper.findById(code);
        if (shortUrl == null) {
            throw new BusinessException("유효하지 않은 링크입니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        return shortUrl;
    }

    @Transactional
    public String markClickAndResolveRedirect(String code) {
        ShortUrlVO shortUrl = getExisting(code);
        ShortUrlPurpose purpose = ShortUrlPurpose.from(shortUrl.getPurpose());
        if (!purpose.countsAsClickRate()) {
            throw new BusinessException("클릭 추적 링크가 아닙니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        shortUrlMapper.markClicked(code);
        return shortUrl.getOriginalUrl();
    }

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

    @Transactional
    public ShortUrlTargetVO unsubscribe(String code) {
        ShortUrlTargetVO target = getUnsubscribeTarget(code);
        shortUrlMapper.markClicked(code);
        unsubscribeMapper.blockCustomerAds(target.getCustomerId(), SYSTEM_ACTOR_ID);
        unsubscribeMapper.revokeSmsConsent(target.getCustomerId(), SYSTEM_ACTOR_ID);
        if (unsubscribeMapper.countRejectHistory(target.getCustomerId()) == 0) {
            unsubscribeMapper.insertRejectHistory(target.getCustomerId(), SYSTEM_ACTOR_ID);
        }
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
                return code;
            } catch (DuplicateKeyException duplicateKeyException) {
                if (i == MAX_GENERATE_ATTEMPTS - 1) {
                    throw duplicateKeyException;
                }
            }
        }
        throw new BusinessException("추적 링크 생성에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
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
