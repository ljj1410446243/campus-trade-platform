package com.campus.trade.item.service;

import com.campus.trade.item.config.RecommendProperties;
import com.campus.trade.item.dto.RecommendationFeedbackRequest;
import com.campus.trade.item.exception.BusinessException;
import com.campus.trade.item.model.RecommendationLog;
import com.campus.trade.item.repository.RecommendationLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class RecommendationLogService {

    public static final String SCENE_HOME_FEED = "HOME_FEED";
    public static final String SCENE_ITEM_SIMILAR = "ITEM_SIMILAR";
    public static final String EVENT_EXPOSE = "EXPOSE";
    public static final String EVENT_CLICK = "CLICK";
    public static final String EVENT_FAVORITE = "FAVORITE";
    public static final String EVENT_CHAT = "CHAT";
    public static final String EVENT_TRADE = "TRADE";

    public static class ExposurePayload {
        private String itemId;
        private List<String> sourceChannels = new ArrayList<>();
        private Integer rank;
        private RecommendationLog.ScoreDetail scores;

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public List<String> getSourceChannels() {
            return sourceChannels;
        }

        public void setSourceChannels(List<String> sourceChannels) {
            this.sourceChannels = sourceChannels;
        }

        public Integer getRank() {
            return rank;
        }

        public void setRank(Integer rank) {
            this.rank = rank;
        }

        public RecommendationLog.ScoreDetail getScores() {
            return scores;
        }

        public void setScores(RecommendationLog.ScoreDetail scores) {
            this.scores = scores;
        }
    }

    private static class RequestSnapshot {
        private String userId;
        private String scene;
        private Set<String> itemIds = new LinkedHashSet<>();

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getScene() {
            return scene;
        }

        public void setScene(String scene) {
            this.scene = scene;
        }

        public Set<String> getItemIds() {
            return itemIds;
        }

        public void setItemIds(Set<String> itemIds) {
            this.itemIds = itemIds;
        }
    }

    private final RecommendationLogRepository recommendationLogRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RecommendProperties recommendProperties;

    public RecommendationLogService(RecommendationLogRepository recommendationLogRepository,
                                    StringRedisTemplate stringRedisTemplate,
                                    ObjectMapper objectMapper,
                                    RecommendProperties recommendProperties) {
        this.recommendationLogRepository = recommendationLogRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.recommendProperties = recommendProperties;
    }

    public String recordExposure(String userId, String scene, List<ExposurePayload> exposures) {
        String requestId = UUID.randomUUID().toString();
        Date now = new Date();
        List<RecommendationLog> logs = new ArrayList<>();
        RequestSnapshot snapshot = new RequestSnapshot();
        snapshot.setUserId(userId);
        snapshot.setScene(scene);

        for (ExposurePayload exposure : exposures) {
            if (exposure == null || exposure.getItemId() == null || exposure.getItemId().isBlank()) {
                continue;
            }
            snapshot.getItemIds().add(exposure.getItemId());

            RecommendationLog log = new RecommendationLog();
            log.setRequestId(requestId);
            log.setUserId(userId);
            log.setScene(scene);
            log.setItemId(exposure.getItemId());
            log.setSourceChannels(exposure.getSourceChannels());
            log.setRank(exposure.getRank());
            log.setScores(exposure.getScores());
            log.setEventType(EVENT_EXPOSE);
            log.setCreatedAt(now);
            logs.add(log);
        }

        if (!logs.isEmpty()) {
            recommendationLogRepository.saveAll(logs);
            cacheSnapshot(requestId, snapshot);
        }
        return requestId;
    }

    public void recordFeedback(String userId, RecommendationFeedbackRequest request) {
        String requestId = normalize(request.getRequestId());
        String scene = normalizeUpper(request.getScene());
        String eventType = normalizeUpper(request.getEventType());
        String itemId = normalize(request.getItemId());

        validateEventType(eventType);
        RequestSnapshot snapshot = loadSnapshot(requestId);
        if (snapshot == null) {
            throw new BusinessException("推荐请求不存在或已过期");
        }
        if (snapshot.getUserId() != null && userId != null && !snapshot.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权回传该推荐请求");
        }
        if (!snapshot.getScene().equals(scene)) {
            throw new BusinessException("推荐场景不匹配");
        }
        if (!snapshot.getItemIds().contains(itemId)) {
            throw new BusinessException("推荐商品不在曝光记录中");
        }

        RecommendationLog log = new RecommendationLog();
        log.setRequestId(requestId);
        log.setUserId(userId);
        log.setScene(scene);
        log.setItemId(itemId);
        log.setEventType(eventType);
        log.setTargetItemId(normalize(request.getTargetItemId()));
        log.setTargetConversationId(normalize(request.getTargetConversationId()));
        log.setTargetTradeId(normalize(request.getTargetTradeId()));
        log.setCreatedAt(new Date());
        recommendationLogRepository.save(log);
    }

    private void validateEventType(String eventType) {
        if (!Set.of(EVENT_CLICK, EVENT_FAVORITE, EVENT_CHAT, EVENT_TRADE).contains(eventType)) {
            throw new BusinessException("不支持的推荐反馈事件");
        }
    }

    private void cacheSnapshot(String requestId, RequestSnapshot snapshot) {
        try {
            Duration ttl = recommendProperties.getCache().getRequestTtl();
            stringRedisTemplate.opsForValue().set(
                    buildRequestKey(requestId),
                    objectMapper.writeValueAsString(snapshot),
                    ttl
            );
        } catch (JsonProcessingException ex) {
            throw new BusinessException("推荐请求缓存失败");
        }
    }

    private RequestSnapshot loadSnapshot(String requestId) {
        String raw = stringRedisTemplate.opsForValue().get(buildRequestKey(requestId));
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, RequestSnapshot.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("推荐请求解析失败");
        }
    }

    private String buildRequestKey(String requestId) {
        return "recommend:request:" + requestId;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
