package com.campus.trade.item.service;

import com.campus.trade.item.config.RecommendProperties;
import com.campus.trade.item.dto.RecommendationFeedbackRequest;
import com.campus.trade.item.dto.SearchItemPageResponse;
import com.campus.trade.item.dto.SearchItemResponse;
import com.campus.trade.item.exception.BusinessException;
import com.campus.trade.item.model.BrowseHistory;
import com.campus.trade.item.model.ChatConversationSnapshot;
import com.campus.trade.item.model.Favorite;
import com.campus.trade.item.model.Item;
import com.campus.trade.item.model.RecommendationLog;
import com.campus.trade.item.model.RecommendationRule;
import com.campus.trade.item.model.TradeSnapshot;
import com.campus.trade.item.model.User;
import com.campus.trade.item.model.UserPreferenceProfile;
import com.campus.trade.item.repository.BrowseHistoryRepository;
import com.campus.trade.item.repository.ChatConversationSnapshotRepository;
import com.campus.trade.item.repository.FavoriteRepository;
import com.campus.trade.item.repository.ItemRepository;
import com.campus.trade.item.repository.TradeSnapshotRepository;
import com.campus.trade.item.util.DistanceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final String STATUS_ON_SALE = "ON_SALE";
    private static final String CHANNEL_PROFILE = "PROFILE";
    private static final String CHANNEL_SIMILAR_USER = "SIMILAR_USER";
    private static final String CHANNEL_RULE = "RULE";
    private static final String CHANNEL_HOT = "HOT";
    private static final String CHANNEL_HOT_FALLBACK = "HOT_FALLBACK";
    private static final String CHANNEL_LATEST_FALLBACK = "LATEST_FALLBACK";
    private static final String CHANNEL_CONTENT = "CONTENT";
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final MongoTemplate mongoTemplate;
    private final ItemRepository itemRepository;
    private final FavoriteRepository favoriteRepository;
    private final BrowseHistoryRepository browseHistoryRepository;
    private final ChatConversationSnapshotRepository chatConversationSnapshotRepository;
    private final TradeSnapshotRepository tradeSnapshotRepository;
    private final RecommendationProfileService recommendationProfileService;
    private final RecommendationRuleService recommendationRuleService;
    private final RecommendationHotService recommendationHotService;
    private final RecommendationLogService recommendationLogService;
    private final RecommendationTextAnalyzer recommendationTextAnalyzer;
    private final RecommendationItemAssembler recommendationItemAssembler;
    private final RecommendProperties recommendProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RecommendationService(MongoTemplate mongoTemplate,
                                 ItemRepository itemRepository,
                                 FavoriteRepository favoriteRepository,
                                 BrowseHistoryRepository browseHistoryRepository,
                                 ChatConversationSnapshotRepository chatConversationSnapshotRepository,
                                 TradeSnapshotRepository tradeSnapshotRepository,
                                 RecommendationProfileService recommendationProfileService,
                                 RecommendationRuleService recommendationRuleService,
                                 RecommendationHotService recommendationHotService,
                                 RecommendationLogService recommendationLogService,
                                 RecommendationTextAnalyzer recommendationTextAnalyzer,
                                 RecommendationItemAssembler recommendationItemAssembler,
                                 RecommendProperties recommendProperties,
                                 StringRedisTemplate stringRedisTemplate,
                                 ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.itemRepository = itemRepository;
        this.favoriteRepository = favoriteRepository;
        this.browseHistoryRepository = browseHistoryRepository;
        this.chatConversationSnapshotRepository = chatConversationSnapshotRepository;
        this.tradeSnapshotRepository = tradeSnapshotRepository;
        this.recommendationProfileService = recommendationProfileService;
        this.recommendationRuleService = recommendationRuleService;
        this.recommendationHotService = recommendationHotService;
        this.recommendationLogService = recommendationLogService;
        this.recommendationTextAnalyzer = recommendationTextAnalyzer;
        this.recommendationItemAssembler = recommendationItemAssembler;
        this.recommendProperties = recommendProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public SearchItemPageResponse recommendItems(String userId,
                                                 Double lat,
                                                 Double lng,
                                                 Integer radiusMeters,
                                                 String sortBy,
                                                 Integer page,
                                                 Integer pageSize,
                                                 Boolean debug) {
        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        boolean debugEnabled = Boolean.TRUE.equals(debug);
        RequestLocation requestLocation = normalizeRequestLocation(lat, lng);
        String cacheKey = buildUserCacheKey(userId, lat, lng, radiusMeters, sortBy, safePage, safePageSize);

        CachedRecommendationPage cachedPage = readCachedPage(cacheKey);
        if (cachedPage == null || cachedPage.getCandidates().isEmpty()) {
            cachedPage = buildHomeRecommendationPage(userId, requestLocation, radiusMeters, sortBy, safePage, safePageSize);
            writeCachedPage(cacheKey, cachedPage, recommendProperties.getCache().getUserTtl().getSeconds());
        }

        SearchItemPageResponse response = materializePage(
                userId,
                RecommendationLogService.SCENE_HOME_FEED,
                cachedPage,
                requestLocation,
                radiusMeters,
                debugEnabled
        );
        if (!response.getList().isEmpty()) {
            return response;
        }

        CachedRecommendationPage rebuiltPage = buildHomeRecommendationPage(userId, requestLocation, radiusMeters, sortBy, safePage, safePageSize);
        writeCachedPage(cacheKey, rebuiltPage, recommendProperties.getCache().getUserTtl().getSeconds());
        return materializePage(userId, RecommendationLogService.SCENE_HOME_FEED, rebuiltPage, requestLocation, radiusMeters, debugEnabled);
    }

    public SearchItemPageResponse similarItems(String userId,
                                               String itemId,
                                               Double lat,
                                               Double lng,
                                               Integer radiusMeters,
                                               Integer page,
                                               Integer pageSize,
                                               Boolean debug) {
        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        boolean debugEnabled = Boolean.TRUE.equals(debug);
        RequestLocation requestLocation = normalizeRequestLocation(lat, lng);
        String cacheKey = buildSimilarCacheKey(userId, itemId, lat, lng, radiusMeters, safePage, safePageSize);

        CachedRecommendationPage cachedPage = readCachedPage(cacheKey);
        if (cachedPage == null || cachedPage.getCandidates().isEmpty()) {
            cachedPage = buildSimilarRecommendationPage(userId, itemId, requestLocation, radiusMeters, safePage, safePageSize);
            writeCachedPage(cacheKey, cachedPage, recommendProperties.getCache().getSimilarTtl().getSeconds());
        }

        return materializePage(userId, RecommendationLogService.SCENE_ITEM_SIMILAR, cachedPage, requestLocation, radiusMeters, debugEnabled);
    }

    public void recordFeedback(String userId, RecommendationFeedbackRequest request) {
        recommendationLogService.recordFeedback(userId, request);
    }

    private CachedRecommendationPage buildHomeRecommendationPage(String userId,
                                                                 RequestLocation requestLocation,
                                                                 Integer radiusMeters,
                                                                 String sortBy,
                                                                 int page,
                                                                 int pageSize) {
        UserPreferenceProfile profile = recommendationProfileService.getOrBuildProfile(userId);
        RecommendationProfileService.UserSignalSummary signalSummary = recommendationProfileService.buildUserSignalSummary(userId);
        boolean coldStart = isColdStart(profile);
        Map<String, CandidateAccumulator> candidates = new LinkedHashMap<>();

        recallByProfile(userId, profile, requestLocation, radiusMeters, candidates);
        if (!coldStart) {
            recallBySimilarUsers(userId, profile, signalSummary, requestLocation, radiusMeters, candidates);
            recallByRules(profile, signalSummary, requestLocation, radiusMeters, candidates);
        }
        recallByHot(profile, requestLocation, radiusMeters, candidates, coldStart);

        Set<String> excludedItemIds = new LinkedHashSet<>(signalSummary.getInteractedItemIds());
        Set<String> recentBrowseIds = signalSummary.getRecentBrowseItemIds();
        List<CandidateAccumulator> ranked = finalizeCandidates(
                candidates,
                userId,
                excludedItemIds,
                recentBrowseIds,
                requestLocation,
                radiusMeters,
                profile,
                null,
                sortBy
        );
        if (shouldApplyFallback(ranked)) {
            ranked = buildHomeFallbackCandidates(
                    userId,
                    profile,
                    signalSummary,
                    requestLocation,
                    radiusMeters,
                    sortBy,
                    coldStart
            );
        }
        return toCachedPage(ranked, page, pageSize, sortBy);
    }

    private CachedRecommendationPage buildSimilarRecommendationPage(String userId,
                                                                    String itemId,
                                                                    RequestLocation requestLocation,
                                                                    Integer radiusMeters,
                                                                    int page,
                                                                    int pageSize) {
        Item baseItem = itemRepository.findById(itemId).orElseThrow(() -> new BusinessException("商品不存在"));
        UserPreferenceProfile profile = userId == null ? null : recommendationProfileService.getOrBuildProfile(userId);
        RecommendationProfileService.UserSignalSummary signalSummary = userId == null
                ? new RecommendationProfileService.UserSignalSummary()
                : recommendationProfileService.buildUserSignalSummary(userId);

        Map<String, CandidateAccumulator> candidates = new LinkedHashMap<>();
        recallSimilarByContent(baseItem, requestLocation, radiusMeters, candidates);
        recallSimilarByRules(baseItem, requestLocation, radiusMeters, candidates);
        recallByHot(profile, requestLocation, radiusMeters, candidates, false);

        Set<String> excludedItemIds = new LinkedHashSet<>(signalSummary.getInteractedItemIds());
        excludedItemIds.add(itemId);

        List<CandidateAccumulator> ranked = finalizeCandidates(
                candidates,
                userId,
                excludedItemIds,
                Set.of(),
                requestLocation,
                radiusMeters,
                profile,
                baseItem,
                null
        );
        return toCachedPage(ranked, page, pageSize, null);
    }

    private SearchItemPageResponse materializePage(String userId,
                                                   String scene,
                                                   CachedRecommendationPage cachedPage,
                                                   RequestLocation requestLocation,
                                                   Integer radiusMeters,
                                                   boolean debugEnabled) {
        if (cachedPage == null || cachedPage.getCandidates().isEmpty()) {
            return new SearchItemPageResponse(UUID.randomUUID().toString(), List.of(), 0);
        }

        List<String> orderedItemIds = cachedPage.getCandidates().stream().map(CachedCandidate::getItemId).toList();
        Map<String, Item> itemMap = itemRepository.findAllById(orderedItemIds).stream()
                .filter(item -> STATUS_ON_SALE.equals(item.getStatus()))
                .collect(Collectors.toMap(Item::getId, item -> item));

        List<Item> orderedItems = orderedItemIds.stream()
                .map(itemMap::get)
                .filter(Objects::nonNull)
                .toList();
        Map<String, Item> orderedItemMap = orderedItems.stream().collect(Collectors.toMap(Item::getId, item -> item));

        Map<String, User> sellerMap = recommendationItemAssembler.loadSellerMap(orderedItems);
        Map<String, com.campus.trade.item.model.FileDocument> fileMap = recommendationItemAssembler.loadFileDocumentMap(orderedItems);

        List<SearchItemResponse> responseList = new ArrayList<>();
        List<RecommendationLogService.ExposurePayload> exposures = new ArrayList<>();

        int rank = 1;
        for (CachedCandidate cachedCandidate : cachedPage.getCandidates()) {
            Item item = orderedItemMap.get(cachedCandidate.getItemId());
            if (item == null) {
                continue;
            }
            if (userId != null && userId.equals(item.getSellerId())) {
                continue;
            }
            Double distanceMeters = resolveDistanceMeters(item, requestLocation);
            if (requestLocation != null && radiusMeters != null && radiusMeters > 0
                    && (distanceMeters == null || distanceMeters > radiusMeters)) {
                continue;
            }

            SearchItemResponse.DebugScores debugScores = debugEnabled ? cachedCandidate.toDebugScores() : null;
            responseList.add(recommendationItemAssembler.toSearchItemResponse(
                    item,
                    distanceMeters,
                    cachedCandidate.getHotScore(),
                    cachedCandidate.getRecommendReason(),
                    cachedCandidate.getSourceChannels(),
                    debugScores,
                    sellerMap,
                    fileMap
            ));

            RecommendationLogService.ExposurePayload exposure = new RecommendationLogService.ExposurePayload();
            exposure.setItemId(item.getId());
            exposure.setRank(rank++);
            exposure.setSourceChannels(cachedCandidate.getSourceChannels());
            exposure.setScores(cachedCandidate.toScoreDetail());
            exposures.add(exposure);
        }

        String requestId = recommendationLogService.recordExposure(userId, scene, exposures);
        return new SearchItemPageResponse(requestId, responseList, cachedPage.getTotal());
    }

    private void recallByProfile(String userId,
                                 UserPreferenceProfile profile,
                                 RequestLocation requestLocation,
                                 Integer radiusMeters,
                                 Map<String, CandidateAccumulator> candidates) {
        List<String> categories = profile.getCategoryPrefs().stream()
                .map(UserPreferenceProfile.WeightedPreference::getKey)
                .filter(this::hasText)
                .limit(3)
                .toList();
        List<String> keywords = profile.getKeywordPrefs().stream()
                .map(UserPreferenceProfile.WeightedPreference::getKey)
                .filter(this::hasText)
                .limit(3)
                .toList();
        List<String> priceBuckets = profile.getPriceRangePrefs().stream()
                .map(UserPreferenceProfile.WeightedPreference::getKey)
                .filter(this::hasText)
                .limit(2)
                .toList();
        List<Integer> conditions = profile.getConditionPrefs().stream()
                .map(UserPreferenceProfile.NumericPreference::getValue)
                .filter(Objects::nonNull)
                .limit(2)
                .toList();
        List<String> tradeModes = profile.getTradeModePrefs().stream()
                .map(UserPreferenceProfile.WeightedPreference::getKey)
                .filter(this::hasText)
                .limit(2)
                .toList();

        if (!categories.isEmpty()) {
            Query query = new Query();
            query.addCriteria(baseOnSaleCriteria()
                    .and("categoryId").in(categories)
                    .and("sellerId").ne(userId));
            applyPriceBucketCriteria(query, priceBuckets.isEmpty() ? null : priceBuckets.get(0));
            if (!conditions.isEmpty()) {
                query.addCriteria(Criteria.where("conditionStar").in(conditions));
            }
            if (!tradeModes.isEmpty()) {
                query.addCriteria(Criteria.where("tradeMode").in(tradeModes));
            }
            query.limit(recommendProperties.getCandidate().getQueryLimit());
            addCandidates(mongoTemplate.find(query, Item.class), candidates, CHANNEL_PROFILE, item -> computeProfileScore(item, profile), requestLocation, radiusMeters);
        }

        for (String keyword : keywords) {
            Query query = new Query();
            query.addCriteria(baseOnSaleCriteria());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(Pattern.quote(keyword), "i"),
                    Criteria.where("description").regex(Pattern.quote(keyword), "i"),
                    Criteria.where("categoryName").regex(Pattern.quote(keyword), "i")
            ));
            query.limit(Math.max(10, recommendProperties.getCandidate().getQueryLimit() / 2));
            addCandidates(mongoTemplate.find(query, Item.class), candidates, CHANNEL_PROFILE, item -> computeProfileScore(item, profile), requestLocation, radiusMeters);
        }
    }

    private void recallBySimilarUsers(String userId,
                                      UserPreferenceProfile profile,
                                      RecommendationProfileService.UserSignalSummary signalSummary,
                                      RequestLocation requestLocation,
                                      Integer radiusMeters,
                                      Map<String, CandidateAccumulator> candidates) {
        List<RecommendationProfileService.SimilarUserScore> similarUsers =
                recommendationProfileService.findTopSimilarUsers(profile, userId, recommendProperties.getSimilarUser().getTopK());
        int behaviorLimit = recommendProperties.getSimilarUser().getMaxBehaviorItemsPerUser();

        for (RecommendationProfileService.SimilarUserScore similarUser : similarUsers) {
            double similarity = similarUser.score();
            if (similarity <= 0D) {
                continue;
            }
            List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(similarUser.userId(), PageRequest.of(0, behaviorLimit));
            List<ChatConversationSnapshot> chats = chatConversationSnapshotRepository.findByBuyerIdOrderByUpdatedAtDesc(similarUser.userId(), PageRequest.of(0, behaviorLimit));
            List<TradeSnapshot> trades = tradeSnapshotRepository.findByBuyerIdOrderByUpdatedAtDesc(similarUser.userId(), PageRequest.of(0, behaviorLimit));

            Map<String, Double> behaviorScores = new HashMap<>();
            for (Favorite favorite : favorites) {
                if (!hasText(favorite.getItemId()) || signalSummary.getInteractedItemIds().contains(favorite.getItemId())) {
                    continue;
                }
                double weight = similarity * 0.7D
                        * recommendationTextAnalyzer.computeDecay(favorite.getCreatedAt(), recommendProperties.getBehavior().getHalfLifeDays());
                behaviorScores.merge(favorite.getItemId(), weight, Double::sum);
            }
            for (ChatConversationSnapshot chat : chats) {
                if (!hasText(chat.getItemId()) || signalSummary.getInteractedItemIds().contains(chat.getItemId())) {
                    continue;
                }
                double weight = similarity * 0.85D
                        * recommendationTextAnalyzer.computeDecay(chat.getUpdatedAt(), recommendProperties.getBehavior().getHalfLifeDays());
                behaviorScores.merge(chat.getItemId(), weight, Double::sum);
            }
            for (TradeSnapshot trade : trades) {
                if (!hasText(trade.getItemId()) || signalSummary.getInteractedItemIds().contains(trade.getItemId())) {
                    continue;
                }
                Date happenedAt = trade.getCompletedAt() == null ? trade.getUpdatedAt() : trade.getCompletedAt();
                double weight = similarity
                        * recommendationTextAnalyzer.computeDecay(happenedAt, recommendProperties.getBehavior().getHalfLifeDays());
                behaviorScores.merge(trade.getItemId(), weight, Double::sum);
            }

            List<Item> items = itemRepository.findAllById(behaviorScores.keySet()).stream()
                    .filter(item -> STATUS_ON_SALE.equals(item.getStatus()))
                    .toList();
            for (Item item : items) {
                if (userId.equals(item.getSellerId())) {
                    continue;
                }
                CandidateAccumulator accumulator = candidates.computeIfAbsent(item.getId(), ignored -> new CandidateAccumulator(item));
                accumulator.item = item;
                accumulator.sourceChannels.add(CHANNEL_SIMILAR_USER);
                accumulator.behaviorSimilarityScore += behaviorScores.getOrDefault(item.getId(), 0D);
            }
        }
    }

    private void recallByRules(UserPreferenceProfile profile,
                               RecommendationProfileService.UserSignalSummary signalSummary,
                               RequestLocation requestLocation,
                               Integer radiusMeters,
                               Map<String, CandidateAccumulator> candidates) {
        String preferredCategoryId = profile.getCategoryPrefs().isEmpty() ? null : profile.getCategoryPrefs().get(0).getKey();
        List<RecommendationRule> itemRules = recommendationRuleService.findRules(
                RecommendationRuleService.SCOPE_ITEM,
                signalSummary.getStrongItemIds(),
                preferredCategoryId,
                recommendProperties.getRule().getMaxRulesPerSeed()
        );
        for (RecommendationRule rule : itemRules) {
            if (!hasText(rule.getConsequentKey()) || signalSummary.getInteractedItemIds().contains(rule.getConsequentKey())) {
                continue;
            }
            itemRepository.findById(rule.getConsequentKey())
                    .filter(item -> STATUS_ON_SALE.equals(item.getStatus()))
                    .ifPresent(item -> {
                        CandidateAccumulator accumulator = candidates.computeIfAbsent(item.getId(), ignored -> new CandidateAccumulator(item));
                        accumulator.item = item;
                        accumulator.sourceChannels.add(CHANNEL_RULE);
                        accumulator.associationScore += associationValue(rule);
                    });
        }

        Set<String> tagSeeds = new LinkedHashSet<>();
        profile.getCategoryPrefs().stream().limit(2).map(pref -> "CATEGORY:" + pref.getKey()).forEach(tagSeeds::add);
        profile.getKeywordPrefs().stream().limit(3).map(pref -> "KEYWORD:" + pref.getKey()).forEach(tagSeeds::add);
        profile.getPriceRangePrefs().stream().limit(1).map(pref -> "PRICE:" + pref.getKey()).forEach(tagSeeds::add);
        profile.getTradeModePrefs().stream().limit(1).map(pref -> "MODE:" + pref.getKey()).forEach(tagSeeds::add);

        List<RecommendationRule> tagRules = recommendationRuleService.findRules(
                RecommendationRuleService.SCOPE_TAG,
                tagSeeds,
                preferredCategoryId,
                recommendProperties.getRule().getMaxRulesPerSeed()
        );
        for (RecommendationRule rule : tagRules) {
            List<Item> items = queryItemsByTag(rule.getConsequentKey(), requestLocation, radiusMeters);
            for (Item item : items) {
                CandidateAccumulator accumulator = candidates.computeIfAbsent(item.getId(), ignored -> new CandidateAccumulator(item));
                accumulator.item = item;
                accumulator.sourceChannels.add(CHANNEL_RULE);
                accumulator.associationScore += associationValue(rule);
            }
        }
    }

    private void recallSimilarByContent(Item baseItem,
                                        RequestLocation requestLocation,
                                        Integer radiusMeters,
                                        Map<String, CandidateAccumulator> candidates) {
        Query query = new Query();
        query.addCriteria(baseOnSaleCriteria());
        List<Criteria> orList = new ArrayList<>();
        if (hasText(baseItem.getCategoryId())) {
            orList.add(Criteria.where("categoryId").is(baseItem.getCategoryId()));
        }
        if (baseItem.getConditionStar() != null) {
            orList.add(Criteria.where("conditionStar").is(baseItem.getConditionStar()));
        }
        if (hasText(baseItem.getTradeMode())) {
            orList.add(Criteria.where("tradeMode").is(baseItem.getTradeMode()));
        }
        List<String> keywords = recommendationTextAnalyzer.extractKeywords(baseItem).stream().limit(3).toList();
        for (String keyword : keywords) {
            orList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(Pattern.quote(keyword), "i"),
                    Criteria.where("description").regex(Pattern.quote(keyword), "i"),
                    Criteria.where("categoryName").regex(Pattern.quote(keyword), "i")
            ));
        }
        if (!orList.isEmpty()) {
            query.addCriteria(new Criteria().orOperator(orList.toArray(new Criteria[0])));
        }
        applyPriceBucketCriteria(query, recommendationTextAnalyzer.resolvePriceBucket(baseItem.getPrice()));
        query.limit(recommendProperties.getCandidate().getQueryLimit());
        addCandidates(mongoTemplate.find(query, Item.class), candidates, CHANNEL_CONTENT, item -> computeItemSimilarity(item, baseItem), requestLocation, radiusMeters);
    }

    private void recallSimilarByRules(Item baseItem,
                                      RequestLocation requestLocation,
                                      Integer radiusMeters,
                                      Map<String, CandidateAccumulator> candidates) {
        List<RecommendationRule> itemRules = recommendationRuleService.findRules(
                RecommendationRuleService.SCOPE_ITEM,
                List.of(baseItem.getId()),
                baseItem.getCategoryId(),
                recommendProperties.getRule().getMaxRulesPerSeed()
        );
        for (RecommendationRule rule : itemRules) {
            if (!hasText(rule.getConsequentKey())) {
                continue;
            }
            itemRepository.findById(rule.getConsequentKey())
                    .filter(item -> STATUS_ON_SALE.equals(item.getStatus()))
                    .ifPresent(item -> {
                        CandidateAccumulator accumulator = candidates.computeIfAbsent(item.getId(), ignored -> new CandidateAccumulator(item));
                        accumulator.item = item;
                        accumulator.sourceChannels.add(CHANNEL_RULE);
                        accumulator.associationScore += associationValue(rule);
                        accumulator.profileScore += computeItemSimilarity(item, baseItem);
                    });
        }

        Set<String> tagSeeds = new LinkedHashSet<>();
        if (hasText(baseItem.getCategoryId())) {
            tagSeeds.add("CATEGORY:" + baseItem.getCategoryId());
        }
        if (hasText(baseItem.getTradeMode())) {
            tagSeeds.add("MODE:" + baseItem.getTradeMode());
        }
        String priceBucket = recommendationTextAnalyzer.resolvePriceBucket(baseItem.getPrice());
        if (hasText(priceBucket)) {
            tagSeeds.add("PRICE:" + priceBucket);
        }
        recommendationTextAnalyzer.extractKeywords(baseItem).stream().limit(3).map(keyword -> "KEYWORD:" + keyword).forEach(tagSeeds::add);

        List<RecommendationRule> tagRules = recommendationRuleService.findRules(
                RecommendationRuleService.SCOPE_TAG,
                tagSeeds,
                baseItem.getCategoryId(),
                recommendProperties.getRule().getMaxRulesPerSeed()
        );
        for (RecommendationRule rule : tagRules) {
            for (Item item : queryItemsByTag(rule.getConsequentKey(), requestLocation, radiusMeters)) {
                CandidateAccumulator accumulator = candidates.computeIfAbsent(item.getId(), ignored -> new CandidateAccumulator(item));
                accumulator.item = item;
                accumulator.sourceChannels.add(CHANNEL_RULE);
                accumulator.associationScore += associationValue(rule);
                accumulator.profileScore += computeItemSimilarity(item, baseItem);
            }
        }
    }

    private void recallByHot(UserPreferenceProfile profile,
                             RequestLocation requestLocation,
                             Integer radiusMeters,
                             Map<String, CandidateAccumulator> candidates,
                             boolean coldStart) {
        String preferredCategory = profile == null || profile.getCategoryPrefs().isEmpty()
                ? null
                : profile.getCategoryPrefs().get(0).getKey();
        List<String> hotItemIds = recommendationHotService.getHotItemIds(preferredCategory, recommendProperties.getCandidate().getHotFallbackCount());
        if (hotItemIds.isEmpty() || coldStart) {
            hotItemIds = recommendationHotService.getHotItemIds(null, recommendProperties.getCandidate().getHotFallbackCount());
        }
        List<Item> items = itemRepository.findAllById(hotItemIds).stream()
                .filter(item -> STATUS_ON_SALE.equals(item.getStatus()))
                .toList();
        addCandidates(items, candidates, CHANNEL_HOT, item -> 0D, requestLocation, radiusMeters);

        if (coldStart) {
            for (String keyword : recommendProperties.getColdStartCategoryKeywords()) {
                if (!hasText(keyword)) {
                    continue;
                }
                Query query = new Query();
                query.addCriteria(baseOnSaleCriteria());
                query.addCriteria(new Criteria().orOperator(
                        Criteria.where("categoryName").regex(Pattern.quote(keyword), "i"),
                        Criteria.where("title").regex(Pattern.quote(keyword), "i"),
                        Criteria.where("description").regex(Pattern.quote(keyword), "i")
                ));
                query.limit(Math.max(10, recommendProperties.getCandidate().getQueryLimit() / 2));
                addCandidates(mongoTemplate.find(query, Item.class), candidates, CHANNEL_HOT, item -> 0D, requestLocation, radiusMeters);
            }
        }
    }

    private List<CandidateAccumulator> buildHomeFallbackCandidates(String userId,
                                                                   UserPreferenceProfile profile,
                                                                   RecommendationProfileService.UserSignalSummary signalSummary,
                                                                   RequestLocation requestLocation,
                                                                   Integer radiusMeters,
                                                                   String sortBy,
                                                                   boolean coldStart) {
        Set<String> relaxedExcludedItemIds = recommendProperties.getCandidate().isAllowRepeatExposureFallback()
                ? Set.of()
                : new LinkedHashSet<>(signalSummary.getInteractedItemIds());

        Map<String, CandidateAccumulator> relaxedCandidates = new LinkedHashMap<>();
        recallByProfile(userId, profile, requestLocation, radiusMeters, relaxedCandidates);
        if (!coldStart) {
            recallBySimilarUsers(userId, profile, signalSummary, requestLocation, radiusMeters, relaxedCandidates);
            recallByRules(profile, signalSummary, requestLocation, radiusMeters, relaxedCandidates);
        }
        recallByHot(profile, requestLocation, radiusMeters, relaxedCandidates, coldStart);
        collectFallbackCatalog(userId, profile, requestLocation, radiusMeters, relaxedCandidates, true);

        List<CandidateAccumulator> relaxedRanked = finalizeCandidates(
                relaxedCandidates,
                userId,
                relaxedExcludedItemIds,
                Set.of(),
                requestLocation,
                radiusMeters,
                profile,
                null,
                sortBy
        );
        if (!shouldApplyFallback(relaxedRanked) || requestLocation == null || radiusMeters == null || radiusMeters <= 0) {
            return relaxedRanked;
        }

        Map<String, CandidateAccumulator> widenedFallbackCandidates = new LinkedHashMap<>();
        collectFallbackCatalog(userId, profile, requestLocation, radiusMeters, widenedFallbackCandidates, false);
        return finalizeCandidates(
                widenedFallbackCandidates,
                userId,
                relaxedExcludedItemIds,
                Set.of(),
                requestLocation,
                radiusMeters,
                profile,
                null,
                sortBy,
                false
        );
    }

    private void collectFallbackCatalog(String userId,
                                        UserPreferenceProfile profile,
                                        RequestLocation requestLocation,
                                        Integer radiusMeters,
                                        Map<String, CandidateAccumulator> candidates,
                                        boolean enforceRadius) {
        String preferredCategory = profile == null || profile.getCategoryPrefs().isEmpty()
                ? null
                : profile.getCategoryPrefs().get(0).getKey();
        int fallbackLimit = Math.max(
                recommendProperties.getCandidate().getHotFallbackCount(),
                recommendProperties.getCandidate().getMinResultCount() * 2
        );

        addHotFallbackCandidates(preferredCategory, requestLocation, radiusMeters, candidates, fallbackLimit, enforceRadius);
        addHotFallbackCandidates(null, requestLocation, radiusMeters, candidates, fallbackLimit, enforceRadius);

        addLatestOnSaleCandidates(userId, preferredCategory, requestLocation, radiusMeters, candidates, fallbackLimit, enforceRadius);
        addLatestOnSaleCandidates(userId, null, requestLocation, radiusMeters, candidates, fallbackLimit, enforceRadius);
    }

    private void addHotFallbackCandidates(String categoryId,
                                          RequestLocation requestLocation,
                                          Integer radiusMeters,
                                          Map<String, CandidateAccumulator> candidates,
                                          int limit,
                                          boolean enforceRadius) {
        List<String> hotItemIds = recommendationHotService.getHotItemIds(categoryId, limit);
        if (hotItemIds.isEmpty()) {
            return;
        }
        List<Item> items = itemRepository.findAllById(hotItemIds).stream()
                .filter(item -> STATUS_ON_SALE.equals(item.getStatus()))
                .toList();
        addCandidates(items, candidates, CHANNEL_HOT_FALLBACK, item -> 0D, requestLocation, radiusMeters, enforceRadius);
    }

    private void addLatestOnSaleCandidates(String userId,
                                           String categoryId,
                                           RequestLocation requestLocation,
                                           Integer radiusMeters,
                                           Map<String, CandidateAccumulator> candidates,
                                           int limit,
                                           boolean enforceRadius) {
        Query query = new Query();
        Criteria criteria = baseOnSaleCriteria();
        if (hasText(userId)) {
            criteria = criteria.and("sellerId").ne(userId);
        }
        if (hasText(categoryId)) {
            criteria = criteria.and("categoryId").is(categoryId);
        }
        query.addCriteria(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        query.limit(limit);
        addCandidates(
                mongoTemplate.find(query, Item.class),
                candidates,
                CHANNEL_LATEST_FALLBACK,
                item -> 0D,
                requestLocation,
                radiusMeters,
                enforceRadius
        );
    }

    private void addCandidates(Collection<Item> items,
                               Map<String, CandidateAccumulator> candidates,
                               String channel,
                               ItemScoreResolver scoreResolver,
                               RequestLocation requestLocation,
                               Integer radiusMeters) {
        addCandidates(items, candidates, channel, scoreResolver, requestLocation, radiusMeters, true);
    }

    private void addCandidates(Collection<Item> items,
                               Map<String, CandidateAccumulator> candidates,
                               String channel,
                               ItemScoreResolver scoreResolver,
                               RequestLocation requestLocation,
                               Integer radiusMeters,
                               boolean enforceRadius) {
        for (Item item : items) {
            if (item == null || !STATUS_ON_SALE.equals(item.getStatus())) {
                continue;
            }
            Double distanceMeters = resolveDistanceMeters(item, requestLocation);
            if (enforceRadius
                    && requestLocation != null
                    && radiusMeters != null
                    && radiusMeters > 0
                    && (distanceMeters == null || distanceMeters > radiusMeters)) {
                continue;
            }
            CandidateAccumulator accumulator = candidates.computeIfAbsent(item.getId(), ignored -> new CandidateAccumulator(item));
            accumulator.item = item;
            accumulator.distanceMeters = distanceMeters;
            accumulator.sourceChannels.add(channel);
            if (CHANNEL_PROFILE.equals(channel) || CHANNEL_CONTENT.equals(channel)) {
                accumulator.profileScore += scoreResolver.resolve(item);
            }
        }
    }

    private List<CandidateAccumulator> finalizeCandidates(Map<String, CandidateAccumulator> candidates,
                                                          String userId,
                                                          Set<String> excludedItemIds,
                                                          Set<String> recentBrowseIds,
                                                          RequestLocation requestLocation,
                                                          Integer radiusMeters,
                                                          UserPreferenceProfile profile,
                                                          Item baseItem,
                                                          String sortBy) {
        return finalizeCandidates(
                candidates,
                userId,
                excludedItemIds,
                recentBrowseIds,
                requestLocation,
                radiusMeters,
                profile,
                baseItem,
                sortBy,
                true
        );
    }

    private List<CandidateAccumulator> finalizeCandidates(Map<String, CandidateAccumulator> candidates,
                                                          String userId,
                                                          Set<String> excludedItemIds,
                                                          Set<String> recentBrowseIds,
                                                          RequestLocation requestLocation,
                                                          Integer radiusMeters,
                                                          UserPreferenceProfile profile,
                                                          Item baseItem,
                                                          String sortBy,
                                                          boolean enforceRadius) {
        Map<String, Double> hotScoreMap = recommendationHotService.getHotScoreMap(candidates.keySet());
        List<CandidateAccumulator> results = new ArrayList<>();

        for (CandidateAccumulator candidate : candidates.values()) {
            Item item = candidate.item;
            if (item == null || !STATUS_ON_SALE.equals(item.getStatus())) {
                continue;
            }
            if (userId != null && userId.equals(item.getSellerId())) {
                continue;
            }
            if (excludedItemIds.contains(item.getId())) {
                continue;
            }
            if (recentBrowseIds.contains(item.getId()) && candidate.sourceChannels.size() == 1 && candidate.sourceChannels.contains(CHANNEL_HOT)) {
                continue;
            }

            candidate.distanceMeters = resolveDistanceMeters(item, requestLocation);
            if (enforceRadius
                    && requestLocation != null
                    && radiusMeters != null
                    && radiusMeters > 0
                    && (candidate.distanceMeters == null || candidate.distanceMeters > radiusMeters)) {
                continue;
            }

            if (baseItem != null) {
                candidate.profileScore += computeItemSimilarity(item, baseItem);
            } else if (profile != null) {
                candidate.profileScore += computeProfileScore(item, profile);
            }

            candidate.hotnessScore = normalizeHotScore(hotScoreMap.get(item.getId()), item);
            candidate.freshnessScore = computeFreshnessScore(item);
            candidate.distanceScore = computeDistanceScore(candidate.distanceMeters, requestLocation);
            candidate.totalScore = computeTotalScore(candidate);
            candidate.hotScore = toHotScoreInt(candidate.hotnessScore, item);
            candidate.recommendReason = resolveReason(candidate.sourceChannels);
            results.add(candidate);
        }

        Comparator<CandidateAccumulator> comparator;
        if ("distance".equalsIgnoreCase(sortBy) && requestLocation != null) {
            comparator = Comparator.comparing(
                            (CandidateAccumulator candidate) -> candidate.distanceMeters,
                            Comparator.nullsLast(Double::compareTo)
                    )
                    .thenComparing(CandidateAccumulator::getTotalScore, Comparator.reverseOrder())
                    .thenComparing(candidate -> candidate.item.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()));
        } else {
            comparator = Comparator.comparingDouble(CandidateAccumulator::getTotalScore).reversed()
                    .thenComparing(candidate -> candidate.item.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()));
        }

        return results.stream()
                .sorted(comparator)
                .limit(recommendProperties.getCandidate().getPoolSize())
                .toList();
    }

    private boolean shouldApplyFallback(List<CandidateAccumulator> ranked) {
        int minResultCount = Math.max(1, recommendProperties.getCandidate().getMinResultCount());
        return ranked == null || ranked.size() < minResultCount;
    }

    private CachedRecommendationPage toCachedPage(List<CandidateAccumulator> ranked, int page, int pageSize, String sortBy) {
        int fromIndex = Math.min((page - 1) * pageSize, ranked.size());
        int toIndex = Math.min(fromIndex + pageSize, ranked.size());
        List<CachedCandidate> candidates = ranked.subList(fromIndex, toIndex).stream()
                .map(CachedCandidate::from)
                .toList();
        CachedRecommendationPage pageResult = new CachedRecommendationPage();
        pageResult.setCandidates(candidates);
        pageResult.setTotal(ranked.size());
        pageResult.setSortBy(sortBy);
        return pageResult;
    }

    private double computeProfileScore(Item item, UserPreferenceProfile profile) {
        if (profile == null) {
            return 0D;
        }
        double score = 0D;
        String categoryId = item.getCategoryId();
        score += findWeight(profile.getCategoryPrefs(), categoryId);
        String priceBucket = recommendationTextAnalyzer.resolvePriceBucket(item.getPrice());
        score += findWeight(profile.getPriceRangePrefs(), priceBucket);
        if (item.getConditionStar() != null) {
            score += profile.getConditionPrefs().stream()
                    .filter(pref -> item.getConditionStar().equals(pref.getValue()))
                    .map(UserPreferenceProfile.NumericPreference::getWeight)
                    .findFirst()
                    .orElse(0D);
        }
        score += findWeight(profile.getTradeModePrefs(), item.getTradeMode());
        Set<String> itemKeywords = new LinkedHashSet<>(recommendationTextAnalyzer.extractKeywords(item));
        score += profile.getKeywordPrefs().stream()
                .filter(pref -> itemKeywords.contains(pref.getKey()))
                .mapToDouble(pref -> pref.getWeight() == null ? 0D : pref.getWeight())
                .sum();
        return round(score);
    }

    private double computeItemSimilarity(Item item, Item baseItem) {
        double score = 0D;
        if (Objects.equals(item.getCategoryId(), baseItem.getCategoryId())) {
            score += 2D;
        }
        String itemPriceBucket = recommendationTextAnalyzer.resolvePriceBucket(item.getPrice());
        String basePriceBucket = recommendationTextAnalyzer.resolvePriceBucket(baseItem.getPrice());
        if (Objects.equals(itemPriceBucket, basePriceBucket)) {
            score += 1D;
        }
        if (Objects.equals(item.getConditionStar(), baseItem.getConditionStar())) {
            score += 0.8D;
        }
        if (Objects.equals(normalize(item.getTradeMode()), normalize(baseItem.getTradeMode()))) {
            score += 0.6D;
        }
        Set<String> itemKeywords = new LinkedHashSet<>(recommendationTextAnalyzer.extractKeywords(item));
        Set<String> baseKeywords = new LinkedHashSet<>(recommendationTextAnalyzer.extractKeywords(baseItem));
        itemKeywords.retainAll(baseKeywords);
        score += itemKeywords.size() * 0.7D;
        return round(score);
    }

    private double normalizeHotScore(Double cachedHotScore, Item item) {
        if (cachedHotScore != null && cachedHotScore > 0D) {
            return round(Math.min(1D, cachedHotScore / 20D));
        }
        int hot = 0;
        if (item.getStats() != null) {
            hot += item.getStats().getViewCount() == null ? 0 : item.getStats().getViewCount();
            hot += item.getStats().getFavoriteCount() == null ? 0 : item.getStats().getFavoriteCount() * 3;
            hot += item.getStats().getChatCount() == null ? 0 : item.getStats().getChatCount() * 4;
        }
        return round(Math.min(1D, hot / 100D));
    }

    private double computeFreshnessScore(Item item) {
        if (item.getCreatedAt() == null) {
            return 0.5D;
        }
        long days = Math.max(0, (System.currentTimeMillis() - item.getCreatedAt().getTime()) / (24L * 3600L * 1000L));
        return round(Math.max(0D, 1D - Math.min(days, 30L) / 30D));
    }

    private double computeDistanceScore(Double distanceMeters, RequestLocation requestLocation) {
        if (requestLocation == null) {
            return 0.5D;
        }
        if (distanceMeters == null) {
            return 0.3D;
        }
        return round(Math.max(0D, 1D - Math.min(distanceMeters, 5000D) / 5000D));
    }

    private double computeTotalScore(CandidateAccumulator candidate) {
        RecommendProperties.Weight weight = recommendProperties.getWeight();
        return round(
                candidate.profileScore * weight.getProfile()
                        + candidate.behaviorSimilarityScore * weight.getBehaviorSimilarity()
                        + candidate.associationScore * weight.getAssociation()
                        + candidate.hotnessScore * weight.getHotness()
                        + candidate.freshnessScore * weight.getFreshness()
                        + candidate.distanceScore * weight.getDistance()
        );
    }

    private List<Item> queryItemsByTag(String tag, RequestLocation requestLocation, Integer radiusMeters) {
        if (!hasText(tag)) {
            return List.of();
        }
        Query query = new Query();
        query.addCriteria(baseOnSaleCriteria());
        if (tag.startsWith("CATEGORY:")) {
            query.addCriteria(Criteria.where("categoryId").is(tag.substring("CATEGORY:".length())));
        } else if (tag.startsWith("KEYWORD:")) {
            String keyword = tag.substring("KEYWORD:".length());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(Pattern.quote(keyword), "i"),
                    Criteria.where("description").regex(Pattern.quote(keyword), "i"),
                    Criteria.where("categoryName").regex(Pattern.quote(keyword), "i")
            ));
        } else if (tag.startsWith("PRICE:")) {
            applyPriceBucketCriteria(query, tag.substring("PRICE:".length()));
        } else if (tag.startsWith("MODE:")) {
            query.addCriteria(Criteria.where("tradeMode").is(tag.substring("MODE:".length())));
        } else if (tag.startsWith("CONDITION:")) {
            query.addCriteria(Criteria.where("conditionStar").is(Integer.parseInt(tag.substring("CONDITION:".length()))));
        } else {
            return List.of();
        }
        query.limit(Math.max(10, recommendProperties.getCandidate().getQueryLimit() / 2));
        List<Item> items = mongoTemplate.find(query, Item.class);
        if (requestLocation == null || radiusMeters == null || radiusMeters <= 0) {
            return items;
        }
        return items.stream()
                .filter(item -> {
                    Double distanceMeters = resolveDistanceMeters(item, requestLocation);
                    return distanceMeters != null && distanceMeters <= radiusMeters;
                })
                .toList();
    }

    private Criteria baseOnSaleCriteria() {
        return Criteria.where("status").is(STATUS_ON_SALE);
    }

    private void applyPriceBucketCriteria(Query query, String priceBucket) {
        if (!hasText(priceBucket)) {
            return;
        }
        Criteria priceCriteria = Criteria.where("price");
        switch (priceBucket) {
            case "0-50" -> priceCriteria.lte(BigDecimal.valueOf(50));
            case "50-100" -> priceCriteria.gt(BigDecimal.valueOf(50)).lte(BigDecimal.valueOf(100));
            case "100-300" -> priceCriteria.gt(BigDecimal.valueOf(100)).lte(BigDecimal.valueOf(300));
            case "300-800" -> priceCriteria.gt(BigDecimal.valueOf(300)).lte(BigDecimal.valueOf(800));
            case "800+" -> priceCriteria.gt(BigDecimal.valueOf(800));
            default -> {
                return;
            }
        }
        query.addCriteria(priceCriteria);
    }

    private String buildUserCacheKey(String userId,
                                     Double lat,
                                     Double lng,
                                     Integer radiusMeters,
                                     String sortBy,
                                     int page,
                                     int pageSize) {
        return String.format(Locale.ROOT, "recommend:user:%s:%d:%d:%s:%s:%s:%s",
                userId, page, pageSize, safeString(lat), safeString(lng), safeString(radiusMeters), normalize(sortBy));
    }

    private String buildSimilarCacheKey(String userId,
                                        String itemId,
                                        Double lat,
                                        Double lng,
                                        Integer radiusMeters,
                                        int page,
                                        int pageSize) {
        String userPart = hasText(userId) ? userId : "anonymous";
        return String.format(Locale.ROOT, "recommend:similar:%s:%s:%d:%d:%s:%s:%s",
                userPart, itemId, page, pageSize, safeString(lat), safeString(lng), safeString(radiusMeters));
    }

    private void writeCachedPage(String key, CachedRecommendationPage page, long ttlSeconds) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(page), java.time.Duration.ofSeconds(Math.max(1L, ttlSeconds)));
        } catch (JsonProcessingException ignored) {
        }
    }

    private CachedRecommendationPage readCachedPage(String key) {
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (!hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<CachedRecommendationPage>() {
            });
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private boolean isColdStart(UserPreferenceProfile profile) {
        if (profile == null || profile.getBehaviorSummary() == null) {
            return true;
        }
        int count = safeInt(profile.getBehaviorSummary().getBrowseCount90d())
                + safeInt(profile.getBehaviorSummary().getFavoriteCount90d())
                + safeInt(profile.getBehaviorSummary().getChatCount90d())
                + safeInt(profile.getBehaviorSummary().getTradeCount90d());
        return count < recommendProperties.getBehavior().getColdStartThreshold();
    }

    private Double resolveDistanceMeters(Item item, RequestLocation requestLocation) {
        if (item == null || requestLocation == null || item.getLocation() == null
                || item.getLocation().getLat() == null || item.getLocation().getLng() == null) {
            return null;
        }
        return DistanceUtil.haversineMeters(
                requestLocation.lat(),
                requestLocation.lng(),
                item.getLocation().getLat(),
                item.getLocation().getLng()
        );
    }

    private RequestLocation normalizeRequestLocation(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }
        return new RequestLocation(lat, lng);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
    }

    private String resolveReason(Set<String> channels) {
        if (channels.contains(CHANNEL_LATEST_FALLBACK)) {
            return "最新在售兜底";
        }
        if (channels.contains(CHANNEL_HOT_FALLBACK)) {
            return "热门兜底";
        }
        if (channels.contains(CHANNEL_PROFILE)) {
            return "画像匹配";
        }
        if (channels.contains(CHANNEL_CONTENT)) {
            return "内容相似";
        }
        if (channels.contains(CHANNEL_SIMILAR_USER)) {
            return "相似用户";
        }
        if (channels.contains(CHANNEL_RULE)) {
            return "关联规则";
        }
        return "热门兜底";
    }

    private double associationValue(RecommendationRule rule) {
        return round(safeDouble(rule.getConfidence()) * 0.7D + safeDouble(rule.getLift()) * 0.3D);
    }

    private double findWeight(List<UserPreferenceProfile.WeightedPreference> prefs, String key) {
        if (!hasText(key) || prefs == null) {
            return 0D;
        }
        return prefs.stream()
                .filter(pref -> key.equals(pref.getKey()))
                .map(UserPreferenceProfile.WeightedPreference::getWeight)
                .findFirst()
                .orElse(0D);
    }

    private int toHotScoreInt(double hotnessScore, Item item) {
        if (item.getStats() != null) {
            int base = safeInt(item.getStats().getViewCount()) + safeInt(item.getStats().getFavoriteCount()) + safeInt(item.getStats().getChatCount());
            if (base > 0) {
                return base;
            }
        }
        return (int) Math.round(hotnessScore * 100);
    }

    private String safeString(Object value) {
        return value == null ? "null" : value.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return "null";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0D : value;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    private record RequestLocation(double lat, double lng) {
    }

    private interface ItemScoreResolver {
        double resolve(Item item);
    }

    private static class CandidateAccumulator {
        private Item item;
        private Set<String> sourceChannels = new LinkedHashSet<>();
        private Double distanceMeters;
        private double profileScore;
        private double behaviorSimilarityScore;
        private double associationScore;
        private double hotnessScore;
        private double freshnessScore;
        private double distanceScore;
        private double totalScore;
        private int hotScore;
        private String recommendReason;

        private CandidateAccumulator(Item item) {
            this.item = item;
        }

        public double getTotalScore() {
            return totalScore;
        }
    }

    public static class CachedRecommendationPage {
        private List<CachedCandidate> candidates = new ArrayList<>();
        private long total;
        private String sortBy;

        public List<CachedCandidate> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<CachedCandidate> candidates) {
            this.candidates = candidates;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public String getSortBy() {
            return sortBy;
        }

        public void setSortBy(String sortBy) {
            this.sortBy = sortBy;
        }
    }

    public static class CachedCandidate {
        private String itemId;
        private List<String> sourceChannels = new ArrayList<>();
        private Double profileScore;
        private Double behaviorSimilarityScore;
        private Double associationScore;
        private Double hotnessScore;
        private Double freshnessScore;
        private Double distanceScore;
        private Double totalScore;
        private Integer hotScore;
        private String recommendReason;

        public static CachedCandidate from(CandidateAccumulator accumulator) {
            CachedCandidate cachedCandidate = new CachedCandidate();
            cachedCandidate.setItemId(accumulator.item.getId());
            cachedCandidate.setSourceChannels(new ArrayList<>(accumulator.sourceChannels));
            cachedCandidate.setProfileScore(accumulator.profileScore);
            cachedCandidate.setBehaviorSimilarityScore(accumulator.behaviorSimilarityScore);
            cachedCandidate.setAssociationScore(accumulator.associationScore);
            cachedCandidate.setHotnessScore(accumulator.hotnessScore);
            cachedCandidate.setFreshnessScore(accumulator.freshnessScore);
            cachedCandidate.setDistanceScore(accumulator.distanceScore);
            cachedCandidate.setTotalScore(accumulator.totalScore);
            cachedCandidate.setHotScore(accumulator.hotScore);
            cachedCandidate.setRecommendReason(accumulator.recommendReason);
            return cachedCandidate;
        }

        public SearchItemResponse.DebugScores toDebugScores() {
            SearchItemResponse.DebugScores debugScores = new SearchItemResponse.DebugScores();
            debugScores.setProfileScore(profileScore);
            debugScores.setBehaviorSimilarityScore(behaviorSimilarityScore);
            debugScores.setAssociationScore(associationScore);
            debugScores.setHotnessScore(hotnessScore);
            debugScores.setFreshnessScore(freshnessScore);
            debugScores.setDistanceScore(distanceScore);
            debugScores.setTotalScore(totalScore);
            return debugScores;
        }

        public RecommendationLog.ScoreDetail toScoreDetail() {
            RecommendationLog.ScoreDetail detail = new RecommendationLog.ScoreDetail();
            detail.setProfileScore(profileScore);
            detail.setBehaviorSimilarityScore(behaviorSimilarityScore);
            detail.setAssociationScore(associationScore);
            detail.setHotnessScore(hotnessScore);
            detail.setFreshnessScore(freshnessScore);
            detail.setDistanceScore(distanceScore);
            detail.setTotalScore(totalScore);
            return detail;
        }

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

        public Double getProfileScore() {
            return profileScore;
        }

        public void setProfileScore(Double profileScore) {
            this.profileScore = profileScore;
        }

        public Double getBehaviorSimilarityScore() {
            return behaviorSimilarityScore;
        }

        public void setBehaviorSimilarityScore(Double behaviorSimilarityScore) {
            this.behaviorSimilarityScore = behaviorSimilarityScore;
        }

        public Double getAssociationScore() {
            return associationScore;
        }

        public void setAssociationScore(Double associationScore) {
            this.associationScore = associationScore;
        }

        public Double getHotnessScore() {
            return hotnessScore;
        }

        public void setHotnessScore(Double hotnessScore) {
            this.hotnessScore = hotnessScore;
        }

        public Double getFreshnessScore() {
            return freshnessScore;
        }

        public void setFreshnessScore(Double freshnessScore) {
            this.freshnessScore = freshnessScore;
        }

        public Double getDistanceScore() {
            return distanceScore;
        }

        public void setDistanceScore(Double distanceScore) {
            this.distanceScore = distanceScore;
        }

        public Double getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(Double totalScore) {
            this.totalScore = totalScore;
        }

        public Integer getHotScore() {
            return hotScore;
        }

        public void setHotScore(Integer hotScore) {
            this.hotScore = hotScore;
        }

        public String getRecommendReason() {
            return recommendReason;
        }

        public void setRecommendReason(String recommendReason) {
            this.recommendReason = recommendReason;
        }
    }
}
