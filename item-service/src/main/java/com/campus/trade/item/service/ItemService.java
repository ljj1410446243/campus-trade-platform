package com.campus.trade.item.service;

import com.campus.trade.item.dto.CreateItemRequest;
import com.campus.trade.item.dto.ItemCommentResponse;
import com.campus.trade.item.dto.ItemDetailResponse;
import com.campus.trade.item.dto.ItemListResponse;
import com.campus.trade.item.dto.RecommendationFeedbackRequest;
import com.campus.trade.item.dto.SearchItemPageResponse;
import com.campus.trade.item.dto.UpdateItemRequest;

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
                                       Double lat,
                                       Double lng,
                                       Integer radiusMeters,
                                       String sortBy,
                                       Integer page,
                                       Integer pageSize);

    SearchItemPageResponse recommendItems(String userId,
                                          Double lat,
                                          Double lng,
                                          Integer radiusMeters,
                                          String sortBy,
                                          Integer page,
                                          Integer pageSize,
                                          Boolean debug);

    SearchItemPageResponse similarItems(String userId,
                                        String itemId,
                                        Double lat,
                                        Double lng,
                                        Integer radiusMeters,
                                        Integer page,
                                        Integer pageSize,
                                        Boolean debug);

    void recordRecommendationFeedback(String userId, RecommendationFeedbackRequest request);

    void addItemComment(String userId, String itemId, String content, Integer rating);

    List<ItemCommentResponse> getItemComments(String itemId);
}
