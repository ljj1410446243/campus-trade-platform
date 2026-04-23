package com.campus.trade.item.service;

import com.campus.trade.item.model.Item;
import com.campus.trade.item.repository.ItemRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ItemStatsReconcileRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ItemStatsReconcileRunner.class);
    private static final String FAVORITES_COLLECTION = "favorites";
    private static final String BROWSE_HISTORY_COLLECTION = "browse_history";
    private static final String CHAT_CONVERSATIONS_COLLECTION = "chat_conversations";
    private static final String ITEM_COLLECTION = "items";

    private final ItemRepository itemRepository;
    private final MongoTemplate mongoTemplate;
    private final ItemCacheInvalidationService itemCacheInvalidationService;
    private final boolean reconcileOnStartup;

    public ItemStatsReconcileRunner(ItemRepository itemRepository,
                                    MongoTemplate mongoTemplate,
                                    ItemCacheInvalidationService itemCacheInvalidationService,
                                    @Value("${app.item-stats.reconcile-on-startup:false}") boolean reconcileOnStartup) {
        this.itemRepository = itemRepository;
        this.mongoTemplate = mongoTemplate;
        this.itemCacheInvalidationService = itemCacheInvalidationService;
        this.reconcileOnStartup = reconcileOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!reconcileOnStartup) {
            return;
        }
        reconcileItemStats();
    }

    private void reconcileItemStats() {
        List<String> itemIds = itemRepository.findAll()
                .stream()
                .map(Item::getId)
                .filter(itemId -> itemId != null && !itemId.isBlank())
                .toList();

        if (itemIds.isEmpty()) {
            log.info("商品统计回填跳过：当前没有商品数据");
            return;
        }

        Map<String, StatsSnapshot> statsByItemId = new HashMap<>();
        itemIds.forEach(itemId -> statsByItemId.put(itemId, new StatsSnapshot()));

        mergeCounts(BROWSE_HISTORY_COLLECTION, statsByItemId, "viewCount");
        mergeCounts(FAVORITES_COLLECTION, statsByItemId, "favoriteCount");
        mergeCounts(CHAT_CONVERSATIONS_COLLECTION, statsByItemId, "chatCount");

        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ITEM_COLLECTION);
        for (String itemId : itemIds) {
            StatsSnapshot snapshot = statsByItemId.getOrDefault(itemId, new StatsSnapshot());
            bulkOperations.updateOne(
                    Query.query(Criteria.where("_id").is(itemId)),
                    new Update()
                            .set("stats.viewCount", snapshot.viewCount())
                            .set("stats.favoriteCount", snapshot.favoriteCount())
                            .set("stats.chatCount", snapshot.chatCount())
            );
        }
        bulkOperations.execute();

        itemCacheInvalidationService.clearAllItemCaches();
        log.info(
                "商品统计回填完成：items={}, viewCountSource={}, favoriteCountSource={}, chatCountSource={}",
                itemIds.size(),
                statsByItemId.values().stream().mapToInt(StatsSnapshot::viewCount).sum(),
                statsByItemId.values().stream().mapToInt(StatsSnapshot::favoriteCount).sum(),
                statsByItemId.values().stream().mapToInt(StatsSnapshot::chatCount).sum()
        );
    }

    private void mergeCounts(String collectionName,
                             Map<String, StatsSnapshot> statsByItemId,
                             String counterType) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("itemId").ne(null)),
                Aggregation.group("itemId").count().as("count")
        );

        List<Document> aggregatedCounts = mongoTemplate.aggregate(aggregation, collectionName, Document.class)
                .getMappedResults();

        for (Document document : aggregatedCounts) {
            String itemId = document.getString("_id");
            if (!statsByItemId.containsKey(itemId)) {
                continue;
            }

            Number countNumber = document.get("count", Number.class);
            int count = countNumber == null ? 0 : countNumber.intValue();
            StatsSnapshot current = statsByItemId.get(itemId);
            statsByItemId.put(itemId, switch (counterType) {
                case "viewCount" -> current.withViewCount(count);
                case "favoriteCount" -> current.withFavoriteCount(count);
                case "chatCount" -> current.withChatCount(count);
                default -> current;
            });
        }
    }

    private record StatsSnapshot(int viewCount, int favoriteCount, int chatCount) {
        private StatsSnapshot() {
            this(0, 0, 0);
        }

        private StatsSnapshot withViewCount(int value) {
            return new StatsSnapshot(value, favoriteCount, chatCount);
        }

        private StatsSnapshot withFavoriteCount(int value) {
            return new StatsSnapshot(viewCount, value, chatCount);
        }

        private StatsSnapshot withChatCount(int value) {
            return new StatsSnapshot(viewCount, favoriteCount, value);
        }
    }
}
