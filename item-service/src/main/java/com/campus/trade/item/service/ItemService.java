package com.campus.trade.item.service;

import com.campus.trade.item.dto.CreateItemRequest;
import com.campus.trade.item.dto.ItemDetailResponse;
import com.campus.trade.item.dto.ItemListResponse;
import com.campus.trade.item.dto.SearchItemPageResponse;
import com.campus.trade.item.dto.UpdateItemRequest;
import com.campus.trade.item.model.ItemComment;

import java.math.BigDecimal;
import java.util.List;

public interface ItemService {

    String ping();

    String createItem(String sellerId, CreateItemRequest request);

    ItemDetailResponse getItemDetail(String itemId);

    List<ItemListResponse> listMyItems(String sellerId);

    ItemDetailResponse updateItem(String sellerId, String itemId, UpdateItemRequest request);

    void offShelfItem(String sellerId, String itemId);

    SearchItemPageResponse searchItems(String q,
                                       String categoryId,
                                       BigDecimal minPrice,
                                       BigDecimal maxPrice,
                                       Integer minCondition,
                                       Integer maxCondition,
                                       String sort,
                                       Integer page,
                                       Integer pageSize);

    SearchItemPageResponse recommendItems(Integer page, Integer pageSize);

    void addItemComment(String userId, String itemId, String comment, Integer rating);

    List<ItemComment> getItemComments(String itemId);
}
