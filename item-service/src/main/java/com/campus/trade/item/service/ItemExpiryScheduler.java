package com.campus.trade.item.service;

import com.campus.trade.item.model.Item;
import com.campus.trade.item.repository.ItemRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class ItemExpiryScheduler {

    private static final String STATUS_ON_SALE = "ON_SALE";
    private static final String STATUS_EXPIRED = "EXPIRED";

    private final ItemRepository itemRepository;
    private final ItemCacheInvalidationService itemCacheInvalidationService;

    public ItemExpiryScheduler(ItemRepository itemRepository, ItemCacheInvalidationService itemCacheInvalidationService) {
        this.itemRepository = itemRepository;
        this.itemCacheInvalidationService = itemCacheInvalidationService;
    }

    @Scheduled(fixedDelay = 300000)
    public void expireItems() {
        Date now = new Date();
        List<Item> expiredItems = itemRepository.findByStatusAndExpireAtLessThanEqual(STATUS_ON_SALE, now);
        if (expiredItems.isEmpty()) {
            return;
        }

        for (Item item : expiredItems) {
            item.setStatus(STATUS_EXPIRED);
            item.setUpdatedAt(now);
            itemRepository.save(item);
            itemCacheInvalidationService.evictItem(item.getId());
        }
    }
}
