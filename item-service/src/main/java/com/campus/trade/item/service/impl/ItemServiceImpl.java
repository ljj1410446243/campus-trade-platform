package com.campus.trade.item.service.impl;

import com.campus.trade.item.dto.CreateItemRequest;
import com.campus.trade.item.dto.ItemDetailResponse;
import com.campus.trade.item.dto.ItemListResponse;
import com.campus.trade.item.dto.SearchItemPageResponse;
import com.campus.trade.item.dto.SearchItemResponse;
import com.campus.trade.item.dto.UpdateItemRequest;
import com.campus.trade.item.exception.BusinessException;
import com.campus.trade.item.model.Item;
import com.campus.trade.item.model.ItemComment;
import com.campus.trade.item.repository.ItemCommentRepository;
import com.campus.trade.item.repository.ItemRepository;
import com.campus.trade.item.service.ItemService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ItemCommentRepository itemCommentRepository;
    private final MongoTemplate mongoTemplate;

    public ItemServiceImpl(ItemRepository itemRepository,
                           ItemCommentRepository itemCommentRepository,
                           MongoTemplate mongoTemplate) {
        this.itemRepository = itemRepository;
        this.itemCommentRepository = itemCommentRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String ping() {
        return "item-service is running";
    }

    @Override
    public String createItem(String sellerId, CreateItemRequest request) {
        Item item = new Item();
        item.setSellerId(sellerId);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategoryId(request.getCategoryId());
        item.setCategoryName(request.getCategoryName());
        item.setPrice(request.getPrice());
        item.setConditionStar(request.getConditionStar());
        item.setImages(request.getImages() == null ? new ArrayList<>() : request.getImages());
        item.setTradeMode(request.getTradeMode());
        item.setStatus("ON_SALE");

        if (request.getLocation() != null) {
            Item.Location location = new Item.Location();
            location.setLat(request.getLocation().getLat());
            location.setLng(request.getLocation().getLng());
            item.setLocation(location);
        }

        Item.Stats stats = new Item.Stats();
        stats.setViewCount(0);
        stats.setFavoriteCount(0);
        stats.setChatCount(0);
        item.setStats(stats);

        Date now = new Date();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        Item saved = itemRepository.save(item);
        return saved.getId();
    }

    @Override
    public ItemDetailResponse getItemDetail(String itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));

        return toDetailResponse(item);
    }

    @Override
    public List<ItemListResponse> listMyItems(String sellerId) {
        return itemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Override
    public ItemDetailResponse updateItem(String sellerId, String itemId, UpdateItemRequest request) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));

        validateOwner(sellerId, item);

        if ("SOLD".equals(item.getStatus())) {
            throw new BusinessException("已售出商品不可编辑");
        }

        if ("OFF_SHELF".equals(item.getStatus())) {
            throw new BusinessException("已下架商品不可编辑");
        }

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategoryId(request.getCategoryId());
        item.setCategoryName(request.getCategoryName());
        item.setPrice(request.getPrice());
        item.setConditionStar(request.getConditionStar());
        item.setTradeMode(request.getTradeMode());
        item.setImages(request.getImages() == null ? new ArrayList<>() : request.getImages());

        if (request.getLocation() != null) {
            Item.Location location = new Item.Location();
            location.setLat(request.getLocation().getLat());
            location.setLng(request.getLocation().getLng());
            item.setLocation(location);
        } else {
            item.setLocation(null);
        }

        item.setUpdatedAt(new Date());

        Item saved = itemRepository.save(item);
        return toDetailResponse(saved);
    }

    @Override
    public void offShelfItem(String sellerId, String itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));

        validateOwner(sellerId, item);

        if ("OFF_SHELF".equals(item.getStatus())) {
            throw new BusinessException("商品已下架");
        }

        if ("SOLD".equals(item.getStatus())) {
            throw new BusinessException("已售出商品不可下架");
        }

        item.setStatus("OFF_SHELF");
        item.setUpdatedAt(new Date());

        itemRepository.save(item);
    }

    @Override
    public SearchItemPageResponse searchItems(String q,
                                              String categoryId,
                                              BigDecimal minPrice,
                                              BigDecimal maxPrice,
                                              Integer minCondition,
                                              Integer maxCondition,
                                              String sort,
                                              Integer page,
                                              Integer pageSize) {

        int safePage = (page == null || page < 1) ? 1 : page;
        int safePageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("status").is("ON_SALE"));

        if (q != null && !q.isBlank()) {
            String escaped = Pattern.quote(q.trim());
            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(escaped, "i"),
                    Criteria.where("description").regex(escaped, "i"),
                    Criteria.where("categoryName").regex(escaped, "i")
            );
            criteriaList.add(keywordCriteria);
        }

        if (categoryId != null && !categoryId.isBlank()) {
            criteriaList.add(Criteria.where("categoryId").is(categoryId));
        }

        if (minPrice != null || maxPrice != null) {
            Criteria priceCriteria = Criteria.where("price");
            if (minPrice != null) {
                priceCriteria.gte(minPrice);
            }
            if (maxPrice != null) {
                priceCriteria.lte(maxPrice);
            }
            criteriaList.add(priceCriteria);
        }

        if (minCondition != null || maxCondition != null) {
            Criteria conditionCriteria = Criteria.where("conditionStar");
            if (minCondition != null) {
                conditionCriteria.gte(minCondition);
            }
            if (maxCondition != null) {
                conditionCriteria.lte(maxCondition);
            }
            criteriaList.add(conditionCriteria);
        }

        query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));

        long total = mongoTemplate.count(query, Item.class);

        Sort mongoSort;
        if ("PRICE".equalsIgnoreCase(sort)) {
            mongoSort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("HOT".equalsIgnoreCase(sort)) {
            mongoSort = Sort.by(Sort.Direction.DESC, "stats.viewCount")
                    .and(Sort.by(Sort.Direction.DESC, "stats.favoriteCount"))
                    .and(Sort.by(Sort.Direction.DESC, "stats.chatCount"));
        } else {
            mongoSort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        query.with(mongoSort);
        query.with(PageRequest.of(safePage - 1, safePageSize));

        List<Item> items = mongoTemplate.find(query, Item.class);
        List<SearchItemResponse> list = items.stream()
                .map(this::toSearchResponse)
                .toList();

        return new SearchItemPageResponse(list, total);
    }

    @Override
    public SearchItemPageResponse recommendItems(Integer page, Integer pageSize) {
        int safePage = (page == null || page < 1) ? 1 : page;
        int safePageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is("ON_SALE"));

        long total = mongoTemplate.count(query, Item.class);

        Sort mongoSort = Sort.by(Sort.Direction.DESC, "stats.viewCount")
                .and(Sort.by(Sort.Direction.DESC, "stats.favoriteCount"))
                .and(Sort.by(Sort.Direction.DESC, "stats.chatCount"))
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));

        query.with(mongoSort);
        query.with(PageRequest.of(safePage - 1, safePageSize));

        List<Item> items = mongoTemplate.find(query, Item.class);
        List<SearchItemResponse> list = items.stream()
                .map(this::toSearchResponse)
                .toList();

        return new SearchItemPageResponse(list, total);
    }

    @Override
    public void addItemComment(String userId, String itemId, String comment, Integer rating) {
        itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));

        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评论评分必须在1到5之间");
        }

        ItemComment itemComment = new ItemComment();
        itemComment.setItemId(itemId);
        itemComment.setUserId(userId);
        itemComment.setUsername("用户");
        itemComment.setComment(comment);
        itemComment.setRating(rating);
        itemComment.setCreatedAt(new Date());

        itemCommentRepository.save(itemComment);
    }

    @Override
    public List<ItemComment> getItemComments(String itemId) {
        itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));

        return itemCommentRepository.findByItemId(itemId);
    }

    private void validateOwner(String sellerId, Item item) {
        if (!sellerId.equals(item.getSellerId())) {
            throw new BusinessException(403, "无权限操作该商品");
        }
    }

    private ItemDetailResponse toDetailResponse(Item item) {
        ItemDetailResponse response = new ItemDetailResponse();
        response.setItemId(item.getId());
        response.setSellerId(item.getSellerId());
        response.setTitle(item.getTitle());
        response.setDescription(item.getDescription());
        response.setCategoryId(item.getCategoryId());
        response.setCategoryName(item.getCategoryName());
        response.setPrice(item.getPrice());
        response.setConditionStar(item.getConditionStar());
        response.setImages(item.getImages());
        response.setTradeMode(item.getTradeMode());
        response.setStatus(item.getStatus());
        response.setCreatedAt(item.getCreatedAt());

        if (item.getLocation() != null) {
            response.setLat(item.getLocation().getLat());
            response.setLng(item.getLocation().getLng());
        }

        return response;
    }

    private ItemListResponse toListResponse(Item item) {
        ItemListResponse response = new ItemListResponse();
        response.setItemId(item.getId());
        response.setTitle(item.getTitle());
        response.setPrice(item.getPrice());
        response.setConditionStar(item.getConditionStar());
        response.setImages(item.getImages());
        response.setStatus(item.getStatus());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }

    private SearchItemResponse toSearchResponse(Item item) {
        SearchItemResponse response = new SearchItemResponse();
        response.setItemId(item.getId());
        response.setTitle(item.getTitle());
        response.setPrice(item.getPrice());
        response.setConditionStar(item.getConditionStar());

        if (item.getImages() != null && !item.getImages().isEmpty()) {
            response.setCoverImage(item.getImages().get(0));
        }

        int hotScore = 0;
        if (item.getStats() != null) {
            hotScore += item.getStats().getViewCount() == null ? 0 : item.getStats().getViewCount();
            hotScore += item.getStats().getFavoriteCount() == null ? 0 : item.getStats().getFavoriteCount();
            hotScore += item.getStats().getChatCount() == null ? 0 : item.getStats().getChatCount();
        }
        response.setHotScore(hotScore);

        return response;
    }
}
