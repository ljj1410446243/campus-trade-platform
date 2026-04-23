package com.campus.trade.item.service;

import com.campus.trade.item.config.RecommendProperties;
import com.campus.trade.item.model.BrowseHistory;
import com.campus.trade.item.model.ChatConversationSnapshot;
import com.campus.trade.item.model.Favorite;
import com.campus.trade.item.model.Item;
import com.campus.trade.item.model.ReviewSnapshot;
import com.campus.trade.item.model.TradeSnapshot;
import com.campus.trade.item.model.UserPreferenceProfile;
import com.campus.trade.item.repository.BrowseHistoryRepository;
import com.campus.trade.item.repository.ChatConversationSnapshotRepository;
import com.campus.trade.item.repository.FavoriteRepository;
import com.campus.trade.item.repository.ItemRepository;
import com.campus.trade.item.repository.ReviewSnapshotRepository;
import com.campus.trade.item.repository.TradeSnapshotRepository;
import com.campus.trade.item.repository.UserPreferenceProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RecommendationProfileService {

    public static class UserSignalSummary {
        private Set<String> interactedItemIds = new LinkedHashSet<>();
        private Set<String> strongItemIds = new LinkedHashSet<>();
        private Set<String> recentBrowseItemIds = new LinkedHashSet<>();

        public Set<String> getInteractedItemIds() {
            return interactedItemIds;
        }

        public void setInteractedItemIds(Set<String> interactedItemIds) {
            this.interactedItemIds = interactedItemIds;
        }

        public Set<String> getStrongItemIds() {
            return strongItemIds;
        }

        public void setStrongItemIds(Set<String> strongItemIds) {
            this.strongItemIds = strongItemIds;
        }

        public Set<String> getRecentBrowseItemIds() {
            return recentBrowseItemIds;
        }

        public void setRecentBrowseItemIds(Set<String> recentBrowseItemIds) {
            this.recentBrowseItemIds = recentBrowseItemIds;
        }
    }

    public record SimilarUserScore(String userId, double score) {
    }

    private static final int PROFILE_ITEM_LIMIT = 300;
    private static final int TOP_CATEGORY_LIMIT = 8;
    private static final int TOP_KEYWORD_LIMIT = 20;
    private static final int TOP_BUCKET_LIMIT = 6;
    private static final int TOP_CONDITION_LIMIT = 5;
    private static final int TOP_TRADE_MODE_LIMIT = 3;

    private final UserPreferenceProfileRepository userPreferenceProfileRepository;
    private final BrowseHistoryRepository browseHistoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final ChatConversationSnapshotRepository chatConversationSnapshotRepository;
    private final TradeSnapshotRepository tradeSnapshotRepository;
    private final ReviewSnapshotRepository reviewSnapshotRepository;
    private final ItemRepository itemRepository;
    private final RecommendationTextAnalyzer recommendationTextAnalyzer;
    private final RecommendProperties recommendProperties;

    public RecommendationProfileService(UserPreferenceProfileRepository userPreferenceProfileRepository,
                                        BrowseHistoryRepository browseHistoryRepository,
                                        FavoriteRepository favoriteRepository,
                                        ChatConversationSnapshotRepository chatConversationSnapshotRepository,
                                        TradeSnapshotRepository tradeSnapshotRepository,
                                        ReviewSnapshotRepository reviewSnapshotRepository,
                                        ItemRepository itemRepository,
                                        RecommendationTextAnalyzer recommendationTextAnalyzer,
                                        RecommendProperties recommendProperties) {
        this.userPreferenceProfileRepository = userPreferenceProfileRepository;
        this.browseHistoryRepository = browseHistoryRepository;
        this.favoriteRepository = favoriteRepository;
        this.chatConversationSnapshotRepository = chatConversationSnapshotRepository;
        this.tradeSnapshotRepository = tradeSnapshotRepository;
        this.reviewSnapshotRepository = reviewSnapshotRepository;
        this.itemRepository = itemRepository;
        this.recommendationTextAnalyzer = recommendationTextAnalyzer;
        this.recommendProperties = recommendProperties;
    }

    public Optional<UserPreferenceProfile> findProfile(String userId) {
        return userPreferenceProfileRepository.findByUserId(userId);
    }

    public UserPreferenceProfile getOrBuildProfile(String userId) {
        return findProfile(userId).orElseGet(() -> rebuildProfile(userId));
    }

    public void rebuildRecentProfiles() {
        Set<String> candidateUserIds = new HashSet<>();
        Date cutoff = new Date(System.currentTimeMillis()
                - recommendProperties.getBehavior().getRecentBehaviorDays() * 24L * 3600L * 1000L);
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(recommendProperties.getBehavior().getRecentBehaviorDays());

        browseHistoryRepository.findByViewedAtAfter(cutoff).stream()
                .map(BrowseHistory::getUserId)
                .filter(this::hasText)
                .forEach(candidateUserIds::add);
        favoriteRepository.findByCreatedAtAfter(cutoff).stream()
                .map(Favorite::getUserId)
                .filter(this::hasText)
                .forEach(candidateUserIds::add);
        chatConversationSnapshotRepository.findByUpdatedAtAfter(cutoffTime).stream()
                .map(ChatConversationSnapshot::getBuyerId)
                .filter(this::hasText)
                .forEach(candidateUserIds::add);
        tradeSnapshotRepository.findByUpdatedAtAfter(cutoff).stream()
                .map(TradeSnapshot::getBuyerId)
                .filter(this::hasText)
                .forEach(candidateUserIds::add);
        reviewSnapshotRepository.findByCreatedAtAfter(cutoff).stream()
                .map(ReviewSnapshot::getFromUserId)
                .filter(this::hasText)
                .forEach(candidateUserIds::add);

        for (String userId : candidateUserIds) {
            rebuildProfile(userId);
        }
    }

    public UserPreferenceProfile rebuildProfile(String userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, PROFILE_ITEM_LIMIT));
        List<BrowseHistory> browses = browseHistoryRepository.findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(0, PROFILE_ITEM_LIMIT));
        List<ChatConversationSnapshot> chats = chatConversationSnapshotRepository.findByBuyerIdOrderByUpdatedAtDesc(userId, PageRequest.of(0, PROFILE_ITEM_LIMIT));
        List<TradeSnapshot> trades = tradeSnapshotRepository.findByBuyerIdOrderByUpdatedAtDesc(userId, PageRequest.of(0, PROFILE_ITEM_LIMIT));
        List<ReviewSnapshot> reviews = reviewSnapshotRepository.findByFromUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, PROFILE_ITEM_LIMIT));

        Map<String, Item> itemMap = loadReferencedItems(favorites, browses, chats, trades, reviews);
        GlobalIdfStats idfStats = buildGlobalIdfStats();
        UserPreferenceProfile profile = userPreferenceProfileRepository.findByUserId(userId).orElseGet(UserPreferenceProfile::new);
        profile.setUserId(userId);

        Map<String, Double> categoryTf = new HashMap<>();
        Map<String, Double> keywordTf = new HashMap<>();
        Map<String, Double> priceTf = new HashMap<>();
        Map<Integer, Double> conditionTf = new HashMap<>();
        Map<String, Double> tradeModeTf = new HashMap<>();
        UserPreferenceProfile.BehaviorSummary behaviorSummary = new UserPreferenceProfile.BehaviorSummary();
        Date lastBehaviorAt = null;

        for (Favorite favorite : favorites) {
            lastBehaviorAt = max(lastBehaviorAt, favorite.getCreatedAt());
            applyBehaviorWindows(behaviorSummary, favorite.getCreatedAt(), "favorite");
            addItemFeatures(itemMap.get(favorite.getItemId()), favorite.getCreatedAt(),
                    recommendProperties.getBehavior().getFavoriteWeight(),
                    categoryTf, keywordTf, priceTf, conditionTf, tradeModeTf);
        }

        Map<String, Long> browseCounts7d = new HashMap<>();
        Date browseStrongCutoff = new Date(System.currentTimeMillis()
                - recommendProperties.getBehavior().getTransactionWindowDays() * 24L * 3600L * 1000L);

        for (BrowseHistory browse : browses) {
            lastBehaviorAt = max(lastBehaviorAt, browse.getViewedAt());
            applyBehaviorWindows(behaviorSummary, browse.getViewedAt(), "browse");
            addItemFeatures(itemMap.get(browse.getItemId()), browse.getViewedAt(),
                    recommendProperties.getBehavior().getBrowseWeight(),
                    categoryTf, keywordTf, priceTf, conditionTf, tradeModeTf);
            if (browse.getViewedAt() != null && browse.getViewedAt().after(browseStrongCutoff) && hasText(browse.getItemId())) {
                browseCounts7d.merge(browse.getItemId(), 1L, Long::sum);
            }
        }

        for (ChatConversationSnapshot chat : chats) {
            Date occurredAt = toDate(chat.getUpdatedAt());
            lastBehaviorAt = max(lastBehaviorAt, occurredAt);
            applyBehaviorWindows(behaviorSummary, occurredAt, "chat");
            addItemFeatures(itemMap.get(chat.getItemId()), occurredAt,
                    recommendProperties.getBehavior().getChatWeight(),
                    categoryTf, keywordTf, priceTf, conditionTf, tradeModeTf);
        }

        for (TradeSnapshot trade : trades) {
            Date occurredAt = trade.getCompletedAt() == null ? trade.getUpdatedAt() : trade.getCompletedAt();
            lastBehaviorAt = max(lastBehaviorAt, occurredAt);
            applyBehaviorWindows(behaviorSummary, occurredAt, "trade");
            addItemFeatures(itemMap.get(trade.getItemId()), occurredAt,
                    recommendProperties.getBehavior().getTradeWeight(),
                    categoryTf, keywordTf, priceTf, conditionTf, tradeModeTf);
        }

        for (ReviewSnapshot review : reviews) {
            if (review.getRating() == null || review.getRating() < recommendProperties.getBehavior().getPositiveReviewThreshold()) {
                continue;
            }
            behaviorSummary.setPositiveReviewCount90d(behaviorSummary.getPositiveReviewCount90d() + 1);
            addItemFeatures(itemMap.get(review.getItemId()), review.getCreatedAt(),
                    recommendProperties.getBehavior().getPositiveReviewBonus(),
                    categoryTf, keywordTf, priceTf, conditionTf, tradeModeTf);
        }

        profile.setBehaviorSummary(behaviorSummary);
        profile.setCategoryPrefs(toWeightedPrefs(categoryTf, idfStats.categoryDf(), idfStats.totalItems(), TOP_CATEGORY_LIMIT));
        profile.setKeywordPrefs(toWeightedPrefs(keywordTf, idfStats.keywordDf(), idfStats.totalItems(), TOP_KEYWORD_LIMIT));
        profile.setPriceRangePrefs(toWeightedPrefs(priceTf, idfStats.priceBucketDf(), idfStats.totalItems(), TOP_BUCKET_LIMIT));
        profile.setConditionPrefs(toConditionPrefs(conditionTf, idfStats.conditionDf(), idfStats.totalItems(), TOP_CONDITION_LIMIT));
        profile.setTradeModePrefs(toWeightedPrefs(tradeModeTf, idfStats.tradeModeDf(), idfStats.totalItems(), TOP_TRADE_MODE_LIMIT));
        profile.setUserVectorNorm(calculateVectorNorm(profile));
        profile.setLastBehaviorAt(lastBehaviorAt);
        profile.setUpdatedAt(new Date());

        return userPreferenceProfileRepository.save(profile);
    }

    public UserSignalSummary buildUserSignalSummary(String userId) {
        UserSignalSummary summary = new UserSignalSummary();
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, PROFILE_ITEM_LIMIT));
        List<BrowseHistory> browses = browseHistoryRepository.findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(0, PROFILE_ITEM_LIMIT));
        List<ChatConversationSnapshot> chats = chatConversationSnapshotRepository.findByBuyerIdOrderByUpdatedAtDesc(userId, PageRequest.of(0, PROFILE_ITEM_LIMIT));
        List<TradeSnapshot> trades = tradeSnapshotRepository.findByBuyerIdOrderByUpdatedAtDesc(userId, PageRequest.of(0, PROFILE_ITEM_LIMIT));

        Date browseStrongCutoff = new Date(System.currentTimeMillis()
                - recommendProperties.getBehavior().getTransactionWindowDays() * 24L * 3600L * 1000L);
        Map<String, Long> browseCounts = new HashMap<>();
        int recentBrowseLimit = recommendProperties.getCandidate().getExcludeRecentBrowseCount();
        int recentBrowseCount = 0;

        for (Favorite favorite : favorites) {
            if (hasText(favorite.getItemId())) {
                summary.getInteractedItemIds().add(favorite.getItemId());
                summary.getStrongItemIds().add(favorite.getItemId());
            }
        }
        for (BrowseHistory browse : browses) {
            if (!hasText(browse.getItemId())) {
                continue;
            }
            summary.getInteractedItemIds().add(browse.getItemId());
            if (recentBrowseCount < recentBrowseLimit) {
                summary.getRecentBrowseItemIds().add(browse.getItemId());
                recentBrowseCount++;
            }
            if (browse.getViewedAt() != null && browse.getViewedAt().after(browseStrongCutoff)) {
                browseCounts.merge(browse.getItemId(), 1L, Long::sum);
            }
        }
        browseCounts.forEach((itemId, count) -> {
            if (count >= recommendProperties.getBehavior().getBrowseStrongThreshold()) {
                summary.getStrongItemIds().add(itemId);
            }
        });
        for (ChatConversationSnapshot chat : chats) {
            if (hasText(chat.getItemId())) {
                summary.getInteractedItemIds().add(chat.getItemId());
                summary.getStrongItemIds().add(chat.getItemId());
            }
        }
        for (TradeSnapshot trade : trades) {
            if (hasText(trade.getItemId())) {
                summary.getInteractedItemIds().add(trade.getItemId());
                summary.getStrongItemIds().add(trade.getItemId());
            }
        }
        return summary;
    }

    public List<SimilarUserScore> findTopSimilarUsers(UserPreferenceProfile baseProfile, String currentUserId, int limit) {
        if (baseProfile == null || baseProfile.getUserVectorNorm() == null || baseProfile.getUserVectorNorm() <= 0D) {
            return List.of();
        }
        Map<String, Double> baseVector = toFeatureVector(baseProfile);
        return userPreferenceProfileRepository.findAll().stream()
                .filter(profile -> profile.getUserId() != null && !profile.getUserId().equals(currentUserId))
                .map(profile -> new SimilarUserScore(profile.getUserId(), cosine(baseVector, profile)))
                .filter(score -> score.score() > 0D)
                .sorted(Comparator.comparingDouble(SimilarUserScore::score).reversed())
                .limit(limit)
                .toList();
    }

    private Map<String, Item> loadReferencedItems(List<Favorite> favorites,
                                                  List<BrowseHistory> browses,
                                                  List<ChatConversationSnapshot> chats,
                                                  List<TradeSnapshot> trades,
                                                  List<ReviewSnapshot> reviews) {
        Set<String> itemIds = new LinkedHashSet<>();
        favorites.stream().map(Favorite::getItemId).filter(this::hasText).forEach(itemIds::add);
        browses.stream().map(BrowseHistory::getItemId).filter(this::hasText).forEach(itemIds::add);
        chats.stream().map(ChatConversationSnapshot::getItemId).filter(this::hasText).forEach(itemIds::add);
        trades.stream().map(TradeSnapshot::getItemId).filter(this::hasText).forEach(itemIds::add);
        reviews.stream().map(ReviewSnapshot::getItemId).filter(this::hasText).forEach(itemIds::add);
        Map<String, Item> itemMap = new HashMap<>();
        itemRepository.findAllById(itemIds).forEach(item -> itemMap.put(item.getId(), item));
        return itemMap;
    }

    private void addItemFeatures(Item item,
                                 Date occurredAt,
                                 int baseWeight,
                                 Map<String, Double> categoryTf,
                                 Map<String, Double> keywordTf,
                                 Map<String, Double> priceTf,
                                 Map<Integer, Double> conditionTf,
                                 Map<String, Double> tradeModeTf) {
        if (item == null || baseWeight <= 0) {
            return;
        }
        double weight = baseWeight * recommendationTextAnalyzer.computeDecay(occurredAt, recommendProperties.getBehavior().getHalfLifeDays());
        if (hasText(item.getCategoryId())) {
            categoryTf.merge(item.getCategoryId(), weight, Double::sum);
        }
        for (String keyword : recommendationTextAnalyzer.extractKeywords(item)) {
            keywordTf.merge(keyword, weight, Double::sum);
        }
        String priceBucket = recommendationTextAnalyzer.resolvePriceBucket(item.getPrice());
        if (hasText(priceBucket)) {
            priceTf.merge(priceBucket, weight, Double::sum);
        }
        if (item.getConditionStar() != null) {
            conditionTf.merge(item.getConditionStar(), weight, Double::sum);
        }
        if (hasText(item.getTradeMode())) {
            tradeModeTf.merge(item.getTradeMode(), weight, Double::sum);
        }
    }

    private void applyBehaviorWindows(UserPreferenceProfile.BehaviorSummary summary, Date occurredAt, String type) {
        if (occurredAt == null) {
            return;
        }
        long diff = Math.max(0, System.currentTimeMillis() - occurredAt.getTime());
        long days = diff / (24L * 3600L * 1000L);
        if (days <= 90) {
            increment(summary, type, 90);
        }
        if (days <= 30) {
            increment(summary, type, 30);
        }
        if (days <= 7) {
            increment(summary, type, 7);
        }
    }

    private void increment(UserPreferenceProfile.BehaviorSummary summary, String type, int windowDays) {
        switch (type) {
            case "browse" -> {
                if (windowDays == 7) {
                    summary.setBrowseCount7d(summary.getBrowseCount7d() + 1);
                } else if (windowDays == 30) {
                    summary.setBrowseCount30d(summary.getBrowseCount30d() + 1);
                } else {
                    summary.setBrowseCount90d(summary.getBrowseCount90d() + 1);
                }
            }
            case "favorite" -> {
                if (windowDays == 7) {
                    summary.setFavoriteCount7d(summary.getFavoriteCount7d() + 1);
                } else if (windowDays == 30) {
                    summary.setFavoriteCount30d(summary.getFavoriteCount30d() + 1);
                } else {
                    summary.setFavoriteCount90d(summary.getFavoriteCount90d() + 1);
                }
            }
            case "chat" -> {
                if (windowDays == 7) {
                    summary.setChatCount7d(summary.getChatCount7d() + 1);
                } else if (windowDays == 30) {
                    summary.setChatCount30d(summary.getChatCount30d() + 1);
                } else {
                    summary.setChatCount90d(summary.getChatCount90d() + 1);
                }
            }
            case "trade" -> {
                if (windowDays == 7) {
                    summary.setTradeCount7d(summary.getTradeCount7d() + 1);
                } else if (windowDays == 30) {
                    summary.setTradeCount30d(summary.getTradeCount30d() + 1);
                } else {
                    summary.setTradeCount90d(summary.getTradeCount90d() + 1);
                }
            }
            default -> {
            }
        }
    }

    private List<UserPreferenceProfile.WeightedPreference> toWeightedPrefs(Map<String, Double> tfMap,
                                                                           Map<String, Integer> dfMap,
                                                                           int totalItems,
                                                                           int limit) {
        List<UserPreferenceProfile.WeightedPreference> prefs = new ArrayList<>();
        recommendationTextAnalyzer.topEntries(applyIdf(tfMap, dfMap, totalItems), limit)
                .forEach(entry -> prefs.add(new UserPreferenceProfile.WeightedPreference(entry.getKey(), recommendationTextAnalyzer.round(entry.getValue()))));
        return prefs;
    }

    private List<UserPreferenceProfile.NumericPreference> toConditionPrefs(Map<Integer, Double> tfMap,
                                                                           Map<Integer, Integer> dfMap,
                                                                           int totalItems,
                                                                           int limit) {
        List<UserPreferenceProfile.NumericPreference> prefs = new ArrayList<>();
        applyIdfNumeric(tfMap, dfMap, totalItems).entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(limit)
                .forEach(entry -> prefs.add(new UserPreferenceProfile.NumericPreference(entry.getKey(), recommendationTextAnalyzer.round(entry.getValue()))));
        return prefs;
    }

    private Map<String, Double> applyIdf(Map<String, Double> tfMap, Map<String, Integer> dfMap, int totalItems) {
        Map<String, Double> result = new LinkedHashMap<>();
        int safeTotalItems = Math.max(1, totalItems);
        for (Map.Entry<String, Double> entry : tfMap.entrySet()) {
            int df = Math.max(1, dfMap.getOrDefault(entry.getKey(), 1));
            double idf = Math.log((safeTotalItems + 1D) / df);
            result.put(entry.getKey(), entry.getValue() * Math.max(idf, 0.1D));
        }
        return result;
    }

    private Map<Integer, Double> applyIdfNumeric(Map<Integer, Double> tfMap, Map<Integer, Integer> dfMap, int totalItems) {
        Map<Integer, Double> result = new LinkedHashMap<>();
        int safeTotalItems = Math.max(1, totalItems);
        for (Map.Entry<Integer, Double> entry : tfMap.entrySet()) {
            int df = Math.max(1, dfMap.getOrDefault(entry.getKey(), 1));
            double idf = Math.log((safeTotalItems + 1D) / df);
            result.put(entry.getKey(), entry.getValue() * Math.max(idf, 0.1D));
        }
        return result;
    }

    private GlobalIdfStats buildGlobalIdfStats() {
        Map<String, Integer> categoryDf = new HashMap<>();
        Map<String, Integer> keywordDf = new HashMap<>();
        Map<String, Integer> priceBucketDf = new HashMap<>();
        Map<Integer, Integer> conditionDf = new HashMap<>();
        Map<String, Integer> tradeModeDf = new HashMap<>();

        List<Item> allItems = itemRepository.findAll();
        for (Item item : allItems) {
            if (item == null) {
                continue;
            }
            if (hasText(item.getCategoryId())) {
                categoryDf.merge(item.getCategoryId(), 1, Integer::sum);
            }
            for (String keyword : new LinkedHashSet<>(recommendationTextAnalyzer.extractKeywords(item))) {
                keywordDf.merge(keyword, 1, Integer::sum);
            }
            String priceBucket = recommendationTextAnalyzer.resolvePriceBucket(item.getPrice());
            if (hasText(priceBucket)) {
                priceBucketDf.merge(priceBucket, 1, Integer::sum);
            }
            if (item.getConditionStar() != null) {
                conditionDf.merge(item.getConditionStar(), 1, Integer::sum);
            }
            if (hasText(item.getTradeMode())) {
                tradeModeDf.merge(item.getTradeMode(), 1, Integer::sum);
            }
        }
        return new GlobalIdfStats(allItems.size(), categoryDf, keywordDf, priceBucketDf, conditionDf, tradeModeDf);
    }

    private double calculateVectorNorm(UserPreferenceProfile profile) {
        double total = 0D;
        for (UserPreferenceProfile.WeightedPreference pref : profile.getCategoryPrefs()) {
            total += square(pref.getWeight());
        }
        for (UserPreferenceProfile.WeightedPreference pref : profile.getKeywordPrefs()) {
            total += square(pref.getWeight());
        }
        for (UserPreferenceProfile.WeightedPreference pref : profile.getPriceRangePrefs()) {
            total += square(pref.getWeight());
        }
        for (UserPreferenceProfile.NumericPreference pref : profile.getConditionPrefs()) {
            total += square(pref.getWeight());
        }
        for (UserPreferenceProfile.WeightedPreference pref : profile.getTradeModePrefs()) {
            total += square(pref.getWeight());
        }
        return Math.sqrt(total);
    }

    private Map<String, Double> toFeatureVector(UserPreferenceProfile profile) {
        Map<String, Double> vector = new LinkedHashMap<>();
        profile.getCategoryPrefs().forEach(pref -> vector.put("CAT:" + pref.getKey(), pref.getWeight()));
        profile.getKeywordPrefs().forEach(pref -> vector.put("KW:" + pref.getKey(), pref.getWeight()));
        profile.getPriceRangePrefs().forEach(pref -> vector.put("PRICE:" + pref.getKey(), pref.getWeight()));
        profile.getConditionPrefs().forEach(pref -> vector.put("COND:" + pref.getValue(), pref.getWeight()));
        profile.getTradeModePrefs().forEach(pref -> vector.put("MODE:" + pref.getKey(), pref.getWeight()));
        return vector;
    }

    private double cosine(Map<String, Double> baseVector, UserPreferenceProfile candidate) {
        if (candidate.getUserVectorNorm() == null || candidate.getUserVectorNorm() <= 0D) {
            return 0D;
        }
        Map<String, Double> candidateVector = toFeatureVector(candidate);
        double dot = 0D;
        for (Map.Entry<String, Double> entry : baseVector.entrySet()) {
            dot += entry.getValue() * candidateVector.getOrDefault(entry.getKey(), 0D);
        }
        double baseNorm = Math.sqrt(baseVector.values().stream().mapToDouble(this::square).sum());
        if (baseNorm <= 0D) {
            return 0D;
        }
        return dot / (baseNorm * candidate.getUserVectorNorm());
    }

    private double square(Double value) {
        double safeValue = value == null ? 0D : value;
        return safeValue * safeValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Date max(Date left, Date right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.after(right) ? left : right;
    }

    private Date toDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    private record GlobalIdfStats(int totalItems,
                                  Map<String, Integer> categoryDf,
                                  Map<String, Integer> keywordDf,
                                  Map<String, Integer> priceBucketDf,
                                  Map<Integer, Integer> conditionDf,
                                  Map<String, Integer> tradeModeDf) {
    }
}
