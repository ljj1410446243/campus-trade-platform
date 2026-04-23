package com.campus.trade.user.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ItemStatsService {

    private static final String ITEM_COLLECTION = "items";

    private final MongoTemplate mongoTemplate;
    private final ItemCacheInvalidationService itemCacheInvalidationService;

    public ItemStatsService(MongoTemplate mongoTemplate,
                            ItemCacheInvalidationService itemCacheInvalidationService) {
        this.mongoTemplate = mongoTemplate;
        this.itemCacheInvalidationService = itemCacheInvalidationService;
    }

    public void incrementViewCount(String itemId) {
        increment(itemId, "stats.viewCount");
    }

    public void incrementFavoriteCount(String itemId) {
        increment(itemId, "stats.favoriteCount");
    }

    public void decrementFavoriteCount(String itemId) {
        Date now = new Date();
        Query decrementQuery = Query.query(
                Criteria.where("_id").is(itemId).and("stats.favoriteCount").gt(0)
        );
        Update decrement = new Update()
                .inc("stats.favoriteCount", -1)
                .set("updatedAt", now);
        mongoTemplate.updateFirst(decrementQuery, decrement, ITEM_COLLECTION);

        Query clampQuery = new Query(new Criteria().andOperator(
                Criteria.where("_id").is(itemId),
                new Criteria().orOperator(
                        Criteria.where("stats.favoriteCount").exists(false),
                        Criteria.where("stats.favoriteCount").lt(0)
                )
        ));
        Update clamp = new Update()
                .set("stats.favoriteCount", 0)
                .set("updatedAt", now);
        mongoTemplate.updateFirst(clampQuery, clamp, ITEM_COLLECTION);

        itemCacheInvalidationService.evictItemCaches(itemId);
    }

    private void increment(String itemId, String field) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(itemId)),
                new Update()
                        .inc(field, 1)
                        .set("updatedAt", new Date()),
                ITEM_COLLECTION
        );
        itemCacheInvalidationService.evictItemCaches(itemId);
    }
}
