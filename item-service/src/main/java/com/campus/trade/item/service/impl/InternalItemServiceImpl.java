package com.campus.trade.item.service.impl;

import com.campus.trade.item.dto.InternalNegotiatedPriceRequest;
import com.campus.trade.item.exception.BusinessException;
import com.campus.trade.item.model.Item;
import com.campus.trade.item.repository.ItemRepository;
import com.campus.trade.item.service.InternalItemService;
import com.campus.trade.item.service.ItemCacheInvalidationService;
import com.campus.trade.item.util.UserAccessGuard;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class InternalItemServiceImpl implements InternalItemService {

    private static final String STATUS_ON_SALE = "ON_SALE";

    private final ItemRepository itemRepository;
    private final ItemCacheInvalidationService itemCacheInvalidationService;
    private final UserAccessGuard userAccessGuard;

    public InternalItemServiceImpl(ItemRepository itemRepository,
                                   ItemCacheInvalidationService itemCacheInvalidationService,
                                   UserAccessGuard userAccessGuard) {
        this.itemRepository = itemRepository;
        this.itemCacheInvalidationService = itemCacheInvalidationService;
        this.userAccessGuard = userAccessGuard;
    }

    @Override
    public void updateNegotiatedPrice(String itemId, InternalNegotiatedPriceRequest request) {
        userAccessGuard.assertWritable(request.getOperatorUserId());

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));

        if (!request.getSellerId().equals(item.getSellerId())) {
            throw new BusinessException(400, "卖家信息不匹配");
        }
        if (!STATUS_ON_SALE.equals(item.getStatus())) {
            throw new BusinessException(400, "当前商品状态不可改价");
        }
        BigDecimal price = request.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "协商价格必须大于0");
        }

        item.setPrice(price);
        item.setUpdatedAt(new Date());
        itemRepository.save(item);
        itemCacheInvalidationService.evictItem(itemId);
    }
}
