package com.campus.trade.item.service;

import com.campus.trade.item.config.RecommendProperties;
import com.campus.trade.item.model.BrowseHistory;
import com.campus.trade.item.model.ChatConversationSnapshot;
import com.campus.trade.item.model.Favorite;
import com.campus.trade.item.model.Item;
import com.campus.trade.item.model.RecommendationRule;
import com.campus.trade.item.model.TradeSnapshot;
import com.campus.trade.item.repository.BrowseHistoryRepository;
import com.campus.trade.item.repository.ChatConversationSnapshotRepository;
import com.campus.trade.item.repository.FavoriteRepository;
import com.campus.trade.item.repository.ItemRepository;
import com.campus.trade.item.repository.RecommendationRuleRepository;
import com.campus.trade.item.repository.TradeSnapshotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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
import java.util.Set;

@Service
public class RecommendationRuleService {

    public static final String SCOPE_ITEM = "ITEM";
    public static final String SCOPE_TAG = "TAG";
    public static final String WINDOW_RECENT_7D = "RECENT_7D";
    public static final String WINDOW_RECENT_30D = "RECENT_30D";
    public static final String WINDOW_GLOBAL = "GLOBAL";

    private static final String STATUS_COMPLETED = "COMPLETED";

    private final RecommendationRuleRepository recommendationRuleRepository;
    private final BrowseHistoryRepository browseHistoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final ChatConversationSnapshotRepository chatConversationSnapshotRepository;
    private final TradeSnapshotRepository tradeSnapshotRepository;
    private final ItemRepository itemRepository;
    private final RecommendationTextAnalyzer recommendationTextAnalyzer;
    private final RecommendProperties recommendProperties;

    public RecommendationRuleService(RecommendationRuleRepository recommendationRuleRepository,
                                     BrowseHistoryRepository browseHistoryRepository,
                                     FavoriteRepository favoriteRepository,
                                     ChatConversationSnapshotRepository chatConversationSnapshotRepository,
                                     TradeSnapshotRepository tradeSnapshotRepository,
                                     ItemRepository itemRepository,
                                     RecommendationTextAnalyzer recommendationTextAnalyzer,
                                     RecommendProperties recommendProperties) {
        this.recommendationRuleRepository = recommendationRuleRepository;
        this.browseHistoryRepository = browseHistoryRepository;
        this.favoriteRepository = favoriteRepository;
        this.chatConversationSnapshotRepository = chatConversationSnapshotRepository;
        this.tradeSnapshotRepository = tradeSnapshotRepository;
        this.itemRepository = itemRepository;
        this.recommendationTextAnalyzer = recommendationTextAnalyzer;
        this.recommendProperties = recommendProperties;
    }

    public void rebuildRules() {
        List<RecommendationRule> rules = new ArrayList<>();
        rules.addAll(buildRulesForWindow(WINDOW_RECENT_7D, 7));
        rules.addAll(buildRulesForWindow(WINDOW_RECENT_30D, 30));
        rules.addAll(buildRulesForWindow(WINDOW_GLOBAL, recommendProperties.getBehavior().getRecentBehaviorDays()));

        recommendationRuleRepository.deleteAll();
        if (!rules.isEmpty()) {
            recommendationRuleRepository.saveAll(rules);
        }
    }

    public List<RecommendationRule> findRules(String scopeType,
                                              Collection<String> antecedentKeys,
                                              String preferredCategoryId,
                                              int limit) {
        Set<String> seedSet = new LinkedHashSet<>();
        if (antecedentKeys != null) {
            antecedentKeys.stream().filter(this::hasText).forEach(seedSet::add);
        }
        if (seedSet.isEmpty()) {
            return List.of();
        }

        List<RecommendationRule> allRules = recommendationRuleRepository.findAll().stream()
                .filter(rule -> scopeType.equals(rule.getScopeType()))
                .filter(rule -> rule.getAntecedentKeys() != null && !rule.getAntecedentKeys().isEmpty())
                .filter(rule -> rule.getAntecedentKeys().stream().anyMatch(seedSet::contains))
                .toList();

        List<RecommendationRule> selected = new ArrayList<>();
        addRulesByWindow(selected, allRules, WINDOW_RECENT_7D, preferredCategoryId, limit);
        addRulesByWindow(selected, allRules, WINDOW_RECENT_30D, preferredCategoryId, limit);
        addRulesByWindow(selected, allRules, WINDOW_GLOBAL, preferredCategoryId, limit);

        return selected.stream()
                .sorted(Comparator.comparingDouble(this::ruleSortScore).reversed())
                .limit(limit)
                .toList();
    }

    private void addRulesByWindow(List<RecommendationRule> selected,
                                  List<RecommendationRule> allRules,
                                  String windowType,
                                  String preferredCategoryId,
                                  int limit) {
        if (selected.size() >= limit) {
            return;
        }
        List<RecommendationRule> current = allRules.stream()
                .filter(rule -> windowType.equals(rule.getWindowType()))
                .filter(rule -> preferredCategoryId == null
                        || preferredCategoryId.isBlank()
                        || preferredCategoryId.equals(rule.getCategoryId())
                        || WINDOW_GLOBAL.equals(windowType))
                .sorted(Comparator.comparingDouble(this::ruleSortScore).reversed())
                .toList();
        for (RecommendationRule rule : current) {
            if (selected.size() >= limit) {
                break;
            }
            selected.add(rule);
        }
    }

    private double ruleSortScore(RecommendationRule rule) {
        return safe(rule.getConfidence()) * 2D + safe(rule.getLift()) + safe(rule.getSupport());
    }

    private List<RecommendationRule> buildRulesForWindow(String windowType, int days) {
        Map<String, Item> itemMap = itemRepository.findAll().stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);
        List<Set<String>> itemTransactions = buildItemTransactions(days);
        List<Set<String>> tagTransactions = itemTransactions.stream()
                .map(transaction -> toTagTransaction(transaction, itemMap))
                .filter(tags -> tags.size() > 1)
                .toList();

        List<RecommendationRule> result = new ArrayList<>();
        result.addAll(mineSingletonRules(itemTransactions, itemMap, windowType, SCOPE_ITEM));
        result.addAll(mineSingletonRules(tagTransactions, itemMap, windowType, SCOPE_TAG));
        return result;
    }

    private List<Set<String>> buildItemTransactions(int days) {
        Date cutoff = new Date(System.currentTimeMillis() - days * 24L * 3600L * 1000L);
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        int bucketDays = recommendProperties.getBehavior().getTransactionWindowDays();
        long bucketMillis = Math.max(1, bucketDays) * 24L * 3600L * 1000L;

        Map<String, Set<String>> transactions = new LinkedHashMap<>();
        Map<String, Long> browseCounts = new HashMap<>();

        for (Favorite favorite : favoriteRepository.findByCreatedAtAfter(cutoff)) {
            appendTransactionItem(transactions, favorite.getUserId(), favorite.getItemId(), favorite.getCreatedAt(), bucketMillis);
        }
        for (ChatConversationSnapshot chat : chatConversationSnapshotRepository.findByUpdatedAtAfter(cutoffTime)) {
            appendTransactionItem(transactions, chat.getBuyerId(), chat.getItemId(), toDate(chat.getUpdatedAt()), bucketMillis);
        }
        for (TradeSnapshot trade : tradeSnapshotRepository.findByUpdatedAtAfter(cutoff)) {
            if (!STATUS_COMPLETED.equalsIgnoreCase(trade.getStatus())) {
                continue;
            }
            Date happenedAt = trade.getCompletedAt() == null ? trade.getUpdatedAt() : trade.getCompletedAt();
            appendTransactionItem(transactions, trade.getBuyerId(), trade.getItemId(), happenedAt, bucketMillis);
        }
        for (BrowseHistory browse : browseHistoryRepository.findByViewedAtAfter(cutoff)) {
            if (!hasText(browse.getUserId()) || !hasText(browse.getItemId()) || browse.getViewedAt() == null) {
                continue;
            }
            String key = browse.getUserId() + "|" + browse.getItemId() + "|" + (browse.getViewedAt().getTime() / bucketMillis);
            browseCounts.merge(key, 1L, Long::sum);
        }
        browseCounts.forEach((key, count) -> {
            if (count < recommendProperties.getBehavior().getBrowseStrongThreshold()) {
                return;
            }
            String[] parts = key.split("\\|");
            if (parts.length < 3) {
                return;
            }
            String transactionKey = parts[0] + "|" + parts[2];
            transactions.computeIfAbsent(transactionKey, ignored -> new LinkedHashSet<>()).add(parts[1]);
        });

        return transactions.values().stream()
                .filter(transaction -> transaction.size() > 1)
                .map(transaction -> (Set<String>) new LinkedHashSet<>(transaction))
                .toList();
    }

    private void appendTransactionItem(Map<String, Set<String>> transactions,
                                       String userId,
                                       String itemId,
                                       Date occurredAt,
                                       long bucketMillis) {
        if (!hasText(userId) || !hasText(itemId) || occurredAt == null) {
            return;
        }
        String transactionKey = userId + "|" + (occurredAt.getTime() / bucketMillis);
        transactions.computeIfAbsent(transactionKey, ignored -> new LinkedHashSet<>()).add(itemId);
    }

    private Set<String> toTagTransaction(Set<String> itemTransaction, Map<String, Item> itemMap) {
        Set<String> tags = new LinkedHashSet<>();
        for (String itemId : itemTransaction) {
            Item item = itemMap.get(itemId);
            if (item == null) {
                continue;
            }
            if (hasText(item.getCategoryId())) {
                tags.add("CATEGORY:" + item.getCategoryId());
            }
            String priceBucket = recommendationTextAnalyzer.resolvePriceBucket(item.getPrice());
            if (hasText(priceBucket)) {
                tags.add("PRICE:" + priceBucket);
            }
            if (item.getConditionStar() != null) {
                tags.add("CONDITION:" + item.getConditionStar());
            }
            if (hasText(item.getTradeMode())) {
                tags.add("MODE:" + item.getTradeMode());
            }
            recommendationTextAnalyzer.extractKeywords(item).forEach(keyword -> tags.add("KEYWORD:" + keyword));
        }
        return tags;
    }

    private List<RecommendationRule> mineSingletonRules(List<Set<String>> transactions,
                                                        Map<String, Item> itemMap,
                                                        String windowType,
                                                        String scopeType) {
        if (transactions.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> singleCounts = new HashMap<>();
        Map<String, Integer> pairCounts = new HashMap<>();

        for (Set<String> transaction : transactions) {
            List<String> items = new ArrayList<>(transaction);
            items.forEach(item -> singleCounts.merge(item, 1, Integer::sum));
            for (int i = 0; i < items.size(); i++) {
                for (int j = 0; j < items.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    pairCounts.merge(items.get(i) + "->" + items.get(j), 1, Integer::sum);
                }
            }
        }

        List<RecommendationRule> rules = new ArrayList<>();
        int transactionCount = transactions.size();
        for (Map.Entry<String, Integer> pairEntry : pairCounts.entrySet()) {
            String[] pair = pairEntry.getKey().split("->");
            if (pair.length != 2) {
                continue;
            }
            String antecedent = pair[0];
            String consequent = pair[1];
            int pairCount = pairEntry.getValue();
            int antecedentCount = singleCounts.getOrDefault(antecedent, 0);
            int consequentCount = singleCounts.getOrDefault(consequent, 0);
            if (antecedentCount == 0 || consequentCount == 0) {
                continue;
            }
            double support = pairCount / (double) transactionCount;
            double confidence = pairCount / (double) antecedentCount;
            double lift = confidence / (consequentCount / (double) transactionCount);

            if (support < recommendProperties.getRule().getMinSupport()
                    || confidence < recommendProperties.getRule().getMinConfidence()
                    || lift < recommendProperties.getRule().getMinLift()) {
                continue;
            }

            RecommendationRule rule = new RecommendationRule();
            rule.setScopeType(scopeType);
            rule.setWindowType(windowType);
            rule.setAntecedentKeys(List.of(antecedent));
            rule.setConsequentKey(consequent);
            rule.setSupport(recommendationTextAnalyzer.round(support));
            rule.setConfidence(recommendationTextAnalyzer.round(confidence));
            rule.setLift(recommendationTextAnalyzer.round(lift));
            if (SCOPE_ITEM.equals(scopeType)) {
                Item antecedentItem = itemMap.get(antecedent);
                Item consequentItem = itemMap.get(consequent);
                String categoryId = antecedentItem != null ? antecedentItem.getCategoryId() : null;
                if (categoryId == null && consequentItem != null) {
                    categoryId = consequentItem.getCategoryId();
                }
                rule.setCategoryId(categoryId);
            } else {
                rule.setCategoryId(extractCategoryFromTag(antecedent, consequent));
            }
            rule.setUpdatedAt(new Date());
            rules.add(rule);
        }

        return rules.stream()
                .sorted(Comparator.comparingDouble(this::ruleScore).reversed())
                .limit(500)
                .toList();
    }

    private double ruleScore(RecommendationRule rule) {
        return safe(rule.getConfidence()) * 2D + safe(rule.getLift()) + safe(rule.getSupport());
    }

    private String extractCategoryFromTag(String antecedent, String consequent) {
        if (antecedent != null && antecedent.startsWith("CATEGORY:")) {
            return antecedent.substring("CATEGORY:".length());
        }
        if (consequent != null && consequent.startsWith("CATEGORY:")) {
            return consequent.substring("CATEGORY:".length());
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Date toDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    private double safe(Double value) {
        return value == null ? 0D : value;
    }
}
