package com.campus.trade.chat.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class ItemCacheInvalidationService {

    private static final String ITEM_DETAIL_CACHE = "item:detail";
    private static final String ITEM_SEARCH_CACHE = "item:search";
    private static final String ITEM_RECOMMEND_CACHE = "item:recommend";

    private final CacheManager cacheManager;

    public ItemCacheInvalidationService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictItemCaches(String itemId) {
        Cache detailCache = cacheManager.getCache(ITEM_DETAIL_CACHE);
        if (detailCache != null) {
            detailCache.evict(itemId);
        }
        clearCache(ITEM_SEARCH_CACHE);
        clearCache(ITEM_RECOMMEND_CACHE);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
