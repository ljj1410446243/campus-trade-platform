package com.campus.trade.chat.service;

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

    public void incrementChatCount(String itemId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(itemId)),
                new Update()
                        .inc("stats.chatCount", 1)
                        .set("updatedAt", new Date()),
                ITEM_COLLECTION
        );
        itemCacheInvalidationService.evictItemCaches(itemId);
    }
}
