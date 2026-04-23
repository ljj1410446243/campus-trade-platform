package com.campus.trade.item.service;

import com.campus.trade.item.config.RecommendProperties;
import com.campus.trade.item.model.BrowseHistory;
import com.campus.trade.item.model.ChatConversationSnapshot;
import com.campus.trade.item.model.Favorite;
import com.campus.trade.item.model.Item;
import com.campus.trade.item.model.ReviewSnapshot;
import com.campus.trade.item.model.TradeSnapshot;
import com.campus.trade.item.repository.BrowseHistoryRepository;
import com.campus.trade.item.repository.ChatConversationSnapshotRepository;
import com.campus.trade.item.repository.FavoriteRepository;
import com.campus.trade.item.repository.ItemRepository;
import com.campus.trade.item.repository.ReviewSnapshotRepository;
import com.campus.trade.item.repository.TradeSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationHotService {

    public static class HotItemScore {
        private String itemId;
        private Double score;

        public HotItemScore() {
        }

        public HotItemScore(String itemId, Double score) {
            this.itemId = itemId;
            this.score = score;
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }

    private static final String STATUS_ON_SALE = "ON_SALE";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ItemRepository itemRepository;
    private final BrowseHistoryRepository browseHistoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final ChatConversationSnapshotRepository chatConversationSnapshotRepository;
    private final TradeSnapshotRepository tradeSnapshotRepository;
    private final ReviewSnapshotRepository reviewSnapshotRepository;
    private final RecommendationTextAnalyzer recommendationTextAnalyzer;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RecommendProperties recommendProperties;

    public RecommendationHotService(ItemRepository itemRepository,
                                    BrowseHistoryRepository browseHistoryRepository,
                                    FavoriteRepository favoriteRepository,
                                    ChatConversationSnapshotRepository chatConversationSnapshotRepository,
                                    TradeSnapshotRepository tradeSnapshotRepository,
                                    ReviewSnapshotRepository reviewSnapshotRepository,
                                    RecommendationTextAnalyzer recommendationTextAnalyzer,
                                    StringRedisTemplate stringRedisTemplate,
                                    ObjectMapper objectMapper,
                                    RecommendProperties recommendProperties) {
        this.itemRepository = itemRepository;
        this.browseHistoryRepository = browseHistoryRepository;
        this.favoriteRepository = favoriteRepository;
        this.chatConversationSnapshotRepository = chatConversationSnapshotRepository;
        this.tradeSnapshotRepository = tradeSnapshotRepository;
        this.reviewSnapshotRepository = reviewSnapshotRepository;
        this.recommendationTextAnalyzer = recommendationTextAnalyzer;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.recommendProperties = recommendProperties;
    }

    public void rebuildHotCaches() {
        Map<String, Item> onSaleItemMap = itemRepository.findAll().stream()
                .filter(item -> STATUS_ON_SALE.equals(item.getStatus()))
                .collect(Collectors.toMap(Item::getId, item -> item));

        if (onSaleItemMap.isEmpty()) {
            return;
        }

        Date cutoff = new Date(System.currentTimeMillis()
                - recommendProperties.getBehavior().getRecentBehaviorDays() * 24L * 3600L * 1000L);

        Map<String, Double> scores = new HashMap<>();
        applyBrowseScores(scores, onSaleItemMap.keySet(), cutoff);
        applyFavoriteScores(scores, onSaleItemMap.keySet(), cutoff);
        applyChatScores(scores, onSaleItemMap.keySet(), cutoff);
        applyTradeScores(scores, onSaleItemMap.keySet(), cutoff);
        applyReviewScores(scores, onSaleItemMap.keySet(), cutoff);

        List<HotItemScore> global = scores.entrySet().stream()
                .filter(entry -> onSaleItemMap.containsKey(entry.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> new HotItemScore(entry.getKey(), round(entry.getValue())))
                .toList();

        writeHotList("recommend:hot:global", global);

        Map<String, List<HotItemScore>> byCategory = new LinkedHashMap<>();
        for (HotItemScore hotItemScore : global) {
            Item item = onSaleItemMap.get(hotItemScore.getItemId());
            if (item == null || item.getCategoryId() == null || item.getCategoryId().isBlank()) {
                continue;
            }
            byCategory.computeIfAbsent(item.getCategoryId(), key -> new ArrayList<>()).add(hotItemScore);
        }

        for (Map.Entry<String, List<HotItemScore>> entry : byCategory.entrySet()) {
            writeHotList("recommend:hot:category:" + entry.getKey(), entry.getValue());
        }
    }

    public Map<String, Double> getHotScoreMap(Collection<String> itemIds) {
        Map<String, Double> result = new HashMap<>();
        if (itemIds == null || itemIds.isEmpty()) {
            return result;
        }
        List<HotItemScore> hotList = loadHotList("recommend:hot:global");
        if (hotList.isEmpty()) {
            rebuildHotCaches();
            hotList = loadHotList("recommend:hot:global");
        }
        for (HotItemScore score : hotList) {
            if (itemIds.contains(score.getItemId())) {
                result.put(score.getItemId(), score.getScore());
            }
        }
        return result;
    }

    public List<String> getHotItemIds(String categoryId, int limit) {
        String key = categoryId == null || categoryId.isBlank()
                ? "recommend:hot:global"
                : "recommend:hot:category:" + categoryId;
        List<HotItemScore> hotList = loadHotList(key);
        if (hotList.isEmpty()) {
            rebuildHotCaches();
            hotList = loadHotList(key);
        }
        if (hotList.isEmpty() && categoryId != null && !categoryId.isBlank()) {
            hotList = loadHotList("recommend:hot:global");
        }
        return hotList.stream()
                .map(HotItemScore::getItemId)
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
    }

    private void applyBrowseScores(Map<String, Double> scores, Set<String> validItemIds, Date cutoff) {
        for (BrowseHistory history : browseHistoryRepository.findByViewedAtAfter(cutoff)) {
            if (history.getItemId() == null || !validItemIds.contains(history.getItemId())) {
                continue;
            }
            double score = recommendProperties.getBehavior().getBrowseWeight()
                    * recommendationTextAnalyzer.computeDecay(history.getViewedAt(), recommendProperties.getBehavior().getHalfLifeDays());
            scores.merge(history.getItemId(), score, Double::sum);
        }
    }

    private void applyFavoriteScores(Map<String, Double> scores, Set<String> validItemIds, Date cutoff) {
        for (Favorite favorite : favoriteRepository.findByCreatedAtAfter(cutoff)) {
            if (favorite.getItemId() == null || !validItemIds.contains(favorite.getItemId())) {
                continue;
            }
            double score = recommendProperties.getBehavior().getFavoriteWeight()
                    * recommendationTextAnalyzer.computeDecay(favorite.getCreatedAt(), recommendProperties.getBehavior().getHalfLifeDays());
            scores.merge(favorite.getItemId(), score, Double::sum);
        }
    }

    private void applyChatScores(Map<String, Double> scores, Set<String> validItemIds, Date cutoff) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(recommendProperties.getBehavior().getRecentBehaviorDays());
        for (ChatConversationSnapshot conversation : chatConversationSnapshotRepository.findByUpdatedAtAfter(cutoffTime)) {
            if (conversation.getItemId() == null || !validItemIds.contains(conversation.getItemId())) {
                continue;
            }
            double score = recommendProperties.getBehavior().getChatWeight()
                    * recommendationTextAnalyzer.computeDecay(conversation.getUpdatedAt(), recommendProperties.getBehavior().getHalfLifeDays());
            scores.merge(conversation.getItemId(), score, Double::sum);
        }
    }

    private void applyTradeScores(Map<String, Double> scores, Set<String> validItemIds, Date cutoff) {
        for (TradeSnapshot trade : tradeSnapshotRepository.findByUpdatedAtAfter(cutoff)) {
            if (trade.getItemId() == null
                    || !validItemIds.contains(trade.getItemId())
                    || !STATUS_COMPLETED.equalsIgnoreCase(trade.getStatus())) {
                continue;
            }
            Date happenedAt = trade.getCompletedAt() == null ? trade.getUpdatedAt() : trade.getCompletedAt();
            double score = recommendProperties.getBehavior().getTradeWeight()
                    * recommendationTextAnalyzer.computeDecay(happenedAt, recommendProperties.getBehavior().getHalfLifeDays());
            scores.merge(trade.getItemId(), score, Double::sum);
        }
    }

    private void applyReviewScores(Map<String, Double> scores, Set<String> validItemIds, Date cutoff) {
        for (ReviewSnapshot review : reviewSnapshotRepository.findByCreatedAtAfter(cutoff)) {
            if (review.getItemId() == null || !validItemIds.contains(review.getItemId())) {
                continue;
            }
            if (review.getRating() == null || review.getRating() < recommendProperties.getBehavior().getPositiveReviewThreshold()) {
                continue;
            }
            double score = recommendProperties.getBehavior().getPositiveReviewBonus()
                    * recommendationTextAnalyzer.computeDecay(review.getCreatedAt(), recommendProperties.getBehavior().getHalfLifeDays());
            scores.merge(review.getItemId(), score, Double::sum);
        }
    }

    private void writeHotList(String key, List<HotItemScore> values) {
        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(values),
                    recommendProperties.getCache().getHotTtl()
            );
        } catch (JsonProcessingException ignored) {
        }
    }

    private List<HotItemScore> loadHotList(String key) {
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<HotItemScore>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}
