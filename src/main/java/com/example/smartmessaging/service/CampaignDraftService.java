package com.example.smartmessaging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.response.CostEstimationResponseDTO;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.example.smartmessaging.mapper.SendPreparationMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 발송 대상자 임시 보관(Draft) 서비스
 *
 * Redis Sorted Set을 사용하여 수신자 ID 목록을 임시 저장합니다.
 * - score = 삽입 순서(index) → 순서가 보장된 페이징 가능
 * - TTL 30분: 사용자가 이탈해도 자동으로 메모리에서 소멸
 *
 * Redis Key 구조: "draft:recipient:{draftId}"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignDraftService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelService channelService;
    private final SendPreparationMapper sendPreparationMapper;
    private final RecipientChannelResolver recipientChannelResolver;

    private static final String KEY_PREFIX = "draft:recipient:";
    private static final Duration TTL = Duration.ofMinutes(30);


    /**
     * ZSET 내 가장 큰 score 값을 찾아 +1 한 값을 반환합니다.
     * 동시성 문제가 발생하더라도 사용자별 격리된 Draft이므로 실질적인 충돌 가능성은 희박합니다.
     */
    private double getNextScore(ZSetOperations<String, Object> zset, String key) {
        Set<ZSetOperations.TypedTuple<Object>> maxTuples = zset.reverseRangeWithScores(key, 0, 0);
        if (maxTuples != null && !maxTuples.isEmpty()) {
            Double maxScore = maxTuples.iterator().next().getScore();
            return maxScore != null ? maxScore + 1.0 : 0.0;
        }
        return 0.0;
    }

    // =============================================
    // 1. 전체 ID 목록 → Redis Sorted Set에 일괄 저장
    // =============================================
    public String saveDraft(Long userId, List<Long> customerIds) {
        String draftId = UUID.randomUUID().toString();
        String key = KEY_PREFIX + userId + ":" + draftId;

        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();

        // score = 삽입 순서(index) 부여 → 항상 동일한 순서로 페이징 가능
        Set<ZSetOperations.TypedTuple<Object>> tuples = new LinkedHashSet<>();
        for (int i = 0; i < customerIds.size(); i++) {
            final Long id = customerIds.get(i);
            final double score = i;
            tuples.add(new DefaultTypedTuple<>(id, score));
        }

        if (!tuples.isEmpty()) {
            zset.add(key, tuples);
            redisTemplate.expire(key, TTL); // TTL 30분 설정
        }

        log.info("[CampaignDraft] 저장 완료 - draftId={}, 총 {}명, TTL=30분", draftId, customerIds.size());
        return draftId;
    }

    // =============================================
    // 1.5. 기존 Redis Sorted Set에 일괄 추가 (Append)
    // =============================================
    public String appendDraft(Long userId, String draftId, List<Long> customerIds) {
        if (draftId == null || draftId.isBlank()) {
            return saveDraft(userId, customerIds);
        }

        String key = KEY_PREFIX + userId + ":" + draftId;
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        double startScore = getNextScore(zset, key);

        Set<ZSetOperations.TypedTuple<Object>> tuples = new LinkedHashSet<>();
        for (int i = 0; i < customerIds.size(); i++) {
            final Long id = customerIds.get(i);
            final double score = startScore + i;
            tuples.add(new DefaultTypedTuple<>(id, score));
        }

        if (!tuples.isEmpty()) {
            zset.add(key, tuples);
            redisTemplate.expire(key, TTL);
        }

        log.info("[CampaignDraft] 병합 완료 - draftId={}, 추가 {}명", draftId, customerIds.size());
        return draftId;
    }

    public String createEmptyDraft(Long userId) {
        String draftId = UUID.randomUUID().toString();
        log.info("[CampaignDraft] 빈 Draft 생성 - userId={}, draftId={}", userId, draftId);
        return draftId;
    }

    // =============================================
    // 2. 현재 페이지에 해당하는 ID만 슬라이싱 (Java 힙 부하 없음)
    // ZRANGE key startIndex endIndex
    // =============================================
    public List<Long> getPagedIds(Long userId, String draftId, int page, int size) {
        String key = KEY_PREFIX + userId + ":" + draftId;
        long start = (long) (page - 1) * size;
        long end = start + size - 1;

        Set<Object> ids = redisTemplate.opsForZSet().range(key, start, end);
        if (ids == null || ids.isEmpty())
            return List.of();

        return ids.stream()
                .map(id -> Long.parseLong(id.toString()))
                .toList();
    }

    public List<Long> getDraftCustomerIds(Long userId, String draftId) {
        if (draftId == null || draftId.isBlank()) {
            return List.of();
        }

        Set<Object> ids = redisTemplate.opsForZSet().range(KEY_PREFIX + userId + ":" + draftId, 0, -1);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return ids.stream()
                .map(id -> Long.parseLong(id.toString()))
                .toList();
    }

    // =============================================
    // 3. 전체 선택 인원 수 조회 (totalCount 페이징 계산용)
    // =============================================
    public long getTotalCount(Long userId, String draftId) {
        Long count = redisTemplate.opsForZSet().size(KEY_PREFIX + userId + ":" + draftId);
        return count != null ? count : 0;
    }

    // =============================================
    // 4. 개별 ID 제거 (체크박스 해제 시 - O(log N))
    // =============================================
    public void removeRecipient(Long userId, String draftId, Long customerId) {
        redisTemplate.opsForZSet().remove(KEY_PREFIX + userId + ":" + draftId, customerId);
        log.debug("[CampaignDraft] 제거 - draftId={}, customerId={}", draftId, customerId);
    }

    // =============================================
    // 5. 개별 ID 추가 (체크박스 선택 시 - O(log N))
    // =============================================
    public void addRecipient(Long userId, String draftId, Long customerId) {
        String key = KEY_PREFIX + userId + ":" + draftId;
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        double score = getNextScore(zset, key);
        zset.add(key, customerId, score);
        redisTemplate.expire(key, TTL);
        log.debug("[CampaignDraft] 추가 - draftId={}, customerId={}", draftId, customerId);
    }

    // =============================================
    // 6. Draft 만료 여부 확인 (TTL -2 = 키 없음)
    // =============================================
    public boolean isExpired(Long userId, String draftId) {
        Long ttl = redisTemplate.getExpire(KEY_PREFIX + userId + ":" + draftId);
        return ttl == null || ttl == -2L;
    }

    // =============================================
    // 7. 발송 완료 또는 취소 시 명시적 삭제
    // =============================================
    public void deleteDraft(Long userId, String draftId) {
        redisTemplate.delete(KEY_PREFIX + userId + ":" + draftId);
        log.info("[CampaignDraft] 삭제 완료 - draftId={}", draftId);
    }

    // =============================================
    // 8. 현재 노출해야 할 고객 ID 목록에 대해 Redis에 임시 저장되어 있는지 여부만 맵으로 부분 조회 (O(log N *
    // pageSize)로 메모리 부하 방지)
    // =============================================
    public Map<Long, Boolean> getRecipientStatusMap(Long userId, String draftId, List<Long> customerIds) {
        if (draftId == null || draftId.isBlank() || customerIds == null || customerIds.isEmpty()) {
            return Map.of();
        }
        String key = KEY_PREFIX + userId + ":" + draftId;

        List<Object> results = redisTemplate
                .executePipelined(new org.springframework.data.redis.core.SessionCallback<Object>() {
                    @Override
                    public Object execute(org.springframework.data.redis.core.RedisOperations operations)
                            throws org.springframework.dao.DataAccessException {
                        org.springframework.data.redis.core.ZSetOperations<String, Object> zsetOps = operations
                                .opsForZSet();
                        for (Long id : customerIds) {
                            zsetOps.score(key, id);
                        }
                        return null;
                    }
                }, redisTemplate.getStringSerializer());

        Map<Long, Boolean> statusMap = new HashMap<>();
        for (int i = 0; i < customerIds.size(); i++) {
            statusMap.put(customerIds.get(i), results.get(i) != null);
        }
        return statusMap;
    }

    // =============================================
    // 9. 예상 비용 산출 및 채널 자동 배정 (최저가 순 또는 우선순위 순)
    // =============================================
    public CostEstimationResponseDTO estimateCost(Long userId, String draftId, List<String> priorities) {
        String key = KEY_PREFIX + userId + ":" + draftId;
        Set<Object> ids = redisTemplate.opsForZSet().range(key, 0, -1);
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.DRAFT_NOT_FOUND);
        }

        List<Long> customerIds = ids.stream()
                .map(id -> Long.parseLong(id.toString()))
                .collect(Collectors.toList());

        List<SendRecipientCandidateVO> recipients = new ArrayList<>();
        int chunkSize = 1000;
        for (int i = 0; i < customerIds.size(); i += chunkSize) {
            List<Long> chunk = customerIds.subList(i, Math.min(customerIds.size(), i + chunkSize));
            List<SendRecipientCandidateVO> chunkRecipients = sendPreparationMapper.findRecipientCandidatesByCustomerIds(chunk);
            if (chunkRecipients != null) {
                recipients.addAll(chunkRecipients);
            }
        }

        List<ChannelVO> activeChannels = new ArrayList<>(channelService.getActiveChannels());
        List<RecipientSendPlan> plans = recipientChannelResolver.resolve(recipients, activeChannels, priorities);

        BigDecimal totalCost = BigDecimal.ZERO;
        Map<String, Integer> channelDistribution = new HashMap<>();

        // 채널 분포 초기화 (모든 활성화된 채널의 카운트를 0으로 채움)
        for (ChannelVO ch : activeChannels) {
            channelDistribution.put(normalizeChannelType(ch.getChannelType()), 0);
        }
        channelDistribution.put("UNASSIGNED", 0);

        Map<Long, String> firstChannelTypeById = activeChannels.stream()
                .collect(Collectors.toMap(ChannelVO::getId, channel -> normalizeChannelType(channel.getChannelType()), (left, right) -> left));

        for (RecipientSendPlan plan : plans) {
            totalCost = totalCost.add(plan.getEstimatedCost() == null ? BigDecimal.ZERO : plan.getEstimatedCost());
            String channelType = firstChannelTypeById.getOrDefault(plan.getFirstChannelId(), "UNASSIGNED");
            channelDistribution.put(channelType, channelDistribution.getOrDefault(channelType, 0) + 1);
        }

        channelDistribution.put("UNASSIGNED", customerIds.size() - plans.size());

        return CostEstimationResponseDTO.builder()
                .totalEstimatedCost(totalCost)
                .channelDistribution(channelDistribution)
                .totalValidRecipients(plans.size())
                .build();
    }

    private String normalizeChannelType(String channelType) {
        if (channelType == null) {
            return "";
        }
        String normalized = channelType.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("KAKAO")) {
            return "KAKAO";
        }
        return normalized;
    }
}
