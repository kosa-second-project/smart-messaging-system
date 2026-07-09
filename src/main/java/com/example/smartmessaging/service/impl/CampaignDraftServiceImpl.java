package com.example.smartmessaging.service.impl;

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
import com.example.smartmessaging.service.repository.SendPreparationMapper;
import com.example.smartmessaging.service.CampaignDraftService;
import com.example.smartmessaging.service.ChannelService;
import com.example.smartmessaging.service.RecipientChannelResolver;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 발송 대상자 임시 보관(Draft) 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignDraftServiceImpl implements CampaignDraftService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelService channelService;
    private final SendPreparationMapper sendPreparationMapper;
    private final RecipientChannelResolver recipientChannelResolver;

    private static final String KEY_PREFIX = "draft:recipient:";
    private static final String STATUS_KEY_PREFIX = "draft:recipient:status:";
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int ZSET_WRITE_CHUNK_SIZE = 1000;

    /**
     * ZSET 내 가장 큰 score 값을 찾아 +1 한 값을 반환합니다.
     */
    private double getNextScore(ZSetOperations<String, Object> zset, String key) {
        Set<ZSetOperations.TypedTuple<Object>> maxTuples = zset.reverseRangeWithScores(key, 0, 0);
        if (maxTuples != null && !maxTuples.isEmpty()) {
            Double maxScore = maxTuples.iterator().next().getScore();
            return maxScore != null ? maxScore + 1.0 : 0.0;
        }
        return 0.0;
    }

    @Override
    public String saveDraft(Long userId, List<Long> customerIds) {
        String draftId = UUID.randomUUID().toString();
        String key = KEY_PREFIX + userId + ":" + draftId;

        if (customerIds != null && !customerIds.isEmpty()) {
            addRecipientsInChunks(key, customerIds, 0.0);
            redisTemplate.expire(key, TTL);
        }

        int savedCount = customerIds == null ? 0 : customerIds.size();
        log.info("[CampaignDraft] 저장 완료 - draftId={}, 총 {}명, TTL=30분", draftId, savedCount);
        return draftId;
    }

    @Override
    public String appendDraft(Long userId, String draftId, List<Long> customerIds) {
        if (draftId == null || draftId.isBlank()) {
            return saveDraft(userId, customerIds);
        }

        String key = KEY_PREFIX + userId + ":" + draftId;
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        double startScore = getNextScore(zset, key);

        if (customerIds != null && !customerIds.isEmpty()) {
            addRecipientsInChunks(key, customerIds, startScore);
            redisTemplate.expire(key, TTL);
        }

        int appendedCount = customerIds == null ? 0 : customerIds.size();
        log.info("[CampaignDraft] 병합 완료 - draftId={}, 추가 {}명", draftId, appendedCount);
        return draftId;
    }

    @Override
    public String createEmptyDraft(Long userId) {
        String draftId = UUID.randomUUID().toString();
        log.info("[CampaignDraft] 빈 Draft 생성 - userId={}, draftId={}", userId, draftId);
        return draftId;
    }

    @Override
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

    @Override
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

    @Override
    public long getTotalCount(Long userId, String draftId) {
        Long count = redisTemplate.opsForZSet().size(KEY_PREFIX + userId + ":" + draftId);
        return count != null ? count : 0;
    }

    @Override
    public void markProcessing(Long userId, String draftId, long expectedCount) {
        String key = statusKey(userId, draftId);
        redisTemplate.opsForHash().put(key, "status", "PROCESSING");
        redisTemplate.opsForHash().put(key, "expectedCount", expectedCount);
        redisTemplate.opsForHash().put(key, "totalCount", 0L);
        redisTemplate.expire(key, TTL);
    }

    @Override
    public void markReady(Long userId, String draftId, long totalCount) {
        String key = statusKey(userId, draftId);
        redisTemplate.opsForHash().put(key, "status", "READY");
        redisTemplate.opsForHash().put(key, "totalCount", totalCount);
        redisTemplate.expire(key, TTL);
    }

    @Override
    public void markFailed(Long userId, String draftId) {
        String key = statusKey(userId, draftId);
        redisTemplate.opsForHash().put(key, "status", "FAILED");
        redisTemplate.expire(key, TTL);
    }

    @Override
    public Map<String, Object> getStatus(Long userId, String draftId) {
        if (draftId == null || draftId.isBlank()) {
            return Map.of("status", "NOT_FOUND", "totalCount", 0L);
        }
        Map<Object, Object> status = redisTemplate.opsForHash().entries(statusKey(userId, draftId));
        if (status == null || status.isEmpty()) {
            return Map.of("status", "READY", "totalCount", getTotalCount(userId, draftId));
        }
        return Map.of(
                "status", String.valueOf(status.getOrDefault("status", "READY")),
                "expectedCount", parseLong(status.get("expectedCount")),
                "totalCount", parseLong(status.get("totalCount"))
        );
    }

    @Override
    public void removeRecipient(Long userId, String draftId, Long customerId) {
        redisTemplate.opsForZSet().remove(KEY_PREFIX + userId + ":" + draftId, customerId);
        log.debug("[CampaignDraft] 제거 - draftId={}, customerId={}", draftId, customerId);
    }

    @Override
    public void addRecipient(Long userId, String draftId, Long customerId) {
        String key = KEY_PREFIX + userId + ":" + draftId;
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        double score = getNextScore(zset, key);
        zset.add(key, customerId, score);
        redisTemplate.expire(key, TTL);
        log.debug("[CampaignDraft] 추가 - draftId={}, customerId={}", draftId, customerId);
    }

    @Override
    public void removeRecipients(Long userId, String draftId, List<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return;
        }
        String key = KEY_PREFIX + userId + ":" + draftId;
        for (int offset = 0; offset < customerIds.size(); offset += ZSET_WRITE_CHUNK_SIZE) {
            int end = Math.min(offset + ZSET_WRITE_CHUNK_SIZE, customerIds.size());
            List<Long> chunk = customerIds.subList(offset, end);
            redisTemplate.opsForZSet().remove(key, chunk.toArray());
        }
        log.debug("[CampaignDraft] 배치 제거 - draftId={}, count={}", draftId, customerIds.size());
    }

    @Override
    public void addRecipients(Long userId, String draftId, List<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return;
        }
        String key = KEY_PREFIX + userId + ":" + draftId;
        double startScore = getNextScore(redisTemplate.opsForZSet(), key);
        addRecipientsInChunks(key, customerIds, startScore);
        redisTemplate.expire(key, TTL);
        log.debug("[CampaignDraft] 배치 추가 - draftId={}, count={}", draftId, customerIds.size());
    }

    private void addRecipientsInChunks(String key, List<Long> customerIds, double startScore) {
        redisTemplate.executePipelined(new org.springframework.data.redis.core.SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> Object execute(org.springframework.data.redis.core.RedisOperations<K, V> operations)
                    throws org.springframework.dao.DataAccessException {
                org.springframework.data.redis.core.ZSetOperations<String, Object> zsetOps = (org.springframework.data.redis.core.ZSetOperations<String, Object>) operations.opsForZSet();
                for (int offset = 0; offset < customerIds.size(); offset += ZSET_WRITE_CHUNK_SIZE) {
                    int end = Math.min(offset + ZSET_WRITE_CHUNK_SIZE, customerIds.size());
                    Set<ZSetOperations.TypedTuple<Object>> tuples = new LinkedHashSet<>(end - offset);
                    for (int i = offset; i < end; i++) {
                        tuples.add(new DefaultTypedTuple<>(customerIds.get(i), startScore + i));
                    }
                    zsetOps.add(key, tuples);
                }
                return null;
            }
        }, redisTemplate.getStringSerializer());
    }

    @Override
    public boolean isExpired(Long userId, String draftId) {
        Long ttl = redisTemplate.getExpire(KEY_PREFIX + userId + ":" + draftId);
        return ttl == null || ttl == -2L;
    }

    @Override
    public void deleteDraft(Long userId, String draftId) {
        redisTemplate.delete(KEY_PREFIX + userId + ":" + draftId);
        redisTemplate.delete(statusKey(userId, draftId));
        log.info("[CampaignDraft] 삭제 완료 - draftId={}", draftId);
    }

    private String statusKey(Long userId, String draftId) {
        return STATUS_KEY_PREFIX + userId + ":" + draftId;
    }

    private long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public Map<Long, Boolean> getRecipientStatusMap(Long userId, String draftId, List<Long> customerIds) {
        if (draftId == null || draftId.isBlank() || customerIds == null || customerIds.isEmpty()) {
            return Map.of();
        }
        String key = KEY_PREFIX + userId + ":" + draftId;

        List<Object> results = redisTemplate
                .executePipelined(new org.springframework.data.redis.core.SessionCallback<Object>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <K, V> Object execute(org.springframework.data.redis.core.RedisOperations<K, V> operations)
                            throws org.springframework.dao.DataAccessException {
                        org.springframework.data.redis.core.ZSetOperations<String, Object> zsetOps = (org.springframework.data.redis.core.ZSetOperations<String, Object>) operations
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

    @Override
    public List<Long> getAllIds(Long userId, String draftId) {
        String key = KEY_PREFIX + userId + ":" + draftId;
        Set<Object> ids = redisTemplate.opsForZSet().range(key, 0, -1);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(id -> Long.parseLong(id.toString()))
                .toList();
    }

    @Override
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
