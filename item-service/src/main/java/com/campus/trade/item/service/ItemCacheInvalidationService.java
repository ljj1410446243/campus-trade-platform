package com.campus.trade.item.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class ItemCacheInvalidationService {

    private static final String DETAIL_CACHE = "item:detail";
    private static final String SEARCH_CACHE = "item:search:v3";
    private static final String RECOMMEND_CACHE = "item:recommend";

    private final CacheManager cacheManager;

    public ItemCacheInvalidationService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictItem(String itemId) {
        Cache detailCache = cacheManager.getCache(DETAIL_CACHE);
        if (detailCache != null) {
            detailCache.evict(itemId);
        }
        clearListCaches();
    }

    public void clearAllItemCaches() {
        clearCache(DETAIL_CACHE);
        clearListCaches();
    }

    public void clearListCaches() {
        clearCache(SEARCH_CACHE);
        clearCache(RECOMMEND_CACHE);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
