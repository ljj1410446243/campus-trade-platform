package com.campus.trade.item.service.impl;

import com.campus.trade.item.dto.CreateItemRequest;
import com.campus.trade.item.dto.ItemCommentResponse;
import com.campus.trade.item.dto.ItemDetailResponse;
import com.campus.trade.item.dto.ItemListResponse;
import com.campus.trade.item.dto.LocationDTO;
import com.campus.trade.item.dto.SearchItemPageResponse;
import com.campus.trade.item.dto.SearchItemResponse;
import com.campus.trade.item.dto.UpdateItemRequest;
import com.campus.trade.item.exception.BusinessException;
import com.campus.trade.item.model.Item;
import com.campus.trade.item.model.ItemComment;
import com.campus.trade.item.model.User;
import com.campus.trade.item.repository.ItemCommentRepository;
import com.campus.trade.item.repository.ItemRepository;
import com.campus.trade.item.repository.UserRepository;
import com.campus.trade.item.service.ItemCacheInvalidationService;
import com.campus.trade.item.service.ItemService;
import com.campus.trade.item.util.CoordinateTransformUtil;
import com.campus.trade.item.util.DistanceUtil;
import com.campus.trade.item.util.UserAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ItemServiceImpl implements ItemService {
    private static final Logger log = LoggerFactory.getLogger(ItemServiceImpl.class);

    private static final int DEFAULT_CREDIT_SCORE = 0;
    private static final int DEFAULT_REVIEW_COUNT = 0;
    private static final double DEFAULT_AVERAGE_RATING = 0D;
    private static final String DEFAULT_CREDIT_LEVEL = "NEW";
    private static final String STATUS_ON_SALE = "ON_SALE";
    private static final String STATUS_SOLD = "SOLD";
    private static final String STATUS_OFF_SHELF = "OFF_SHELF";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String COORD_TYPE_WGS84 = "WGS84";
    private static final String COORD_TYPE_GCJ02 = "GCJ02";
    private static final String DEFAULT_LOCATION_SOURCE = "manual";
    private static final String SORT_BY_DISTANCE = "distance";
    private static final String TRADE_MODE_ONLINE = "ONLINE";
    private static final String TRADE_MODE_OFFLINE = "OFFLINE";
    private static final String TRADE_MODE_BOTH = "BOTH";
    private static final String IMAGE_URL_FIELD = "url";

    private final ItemRepository itemRepository;
    private final ItemCommentRepository itemCommentRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final UserAccessGuard userAccessGuard;
    private final ItemCacheInvalidationService itemCacheInvalidationService;

    public ItemServiceImpl(ItemRepository itemRepository,
                           ItemCommentRepository itemCommentRepository,
                           UserRepository userRepository,
                           MongoTemplate mongoTemplate,
                           UserAccessGuard userAccessGuard,
                           ItemCacheInvalidationService itemCacheInvalidationService) {
        this.itemRepository = itemRepository;
        this.itemCommentRepository = itemCommentRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.userAccessGuard = userAccessGuard;
        this.itemCacheInvalidationService = itemCacheInvalidationService;
    }

    @Override
    public String ping() {
        return "item-service is running";
    }

    @Override
    public String createItem(String sellerId, CreateItemRequest request) {
        userAccessGuard.assertWritable(sellerId);
        Item item = new Item();
        item.setSellerId(sellerId);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategoryId(request.getCategoryId());
        item.setCategoryName(request.getCategoryName());
        item.setPrice(request.getPrice());
        item.setConditionStar(request.getConditionStar());
        List<String> images = normalizeImages(request.getImages());
        item.setImages(images);
        item.setCoverImage(resolveCoverImage(images));
        item.setTradeMode(normalizeTradeMode(request.getTradeMode()));
        item.setExpireAt(request.getExpireAt());
        item.setStatus(STATUS_ON_SALE);
        item.setLocation(normalizeLocation(request.getLocation()));

        Item.Stats stats = new Item.Stats();
        stats.setViewCount(0);
        stats.setFavoriteCount(0);
        stats.setChatCount(0);
        item.setStats(stats);

        Date now = new Date();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        Item saved = itemRepository.save(item);
        itemCacheInvalidationService.evictItem(saved.getId());
        return saved.getId();
    }

    @Override
    @Cacheable(cacheNames = "item:detail", key = "#itemId")
    public ItemDetailResponse getItemDetail(String itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));

        return toDetailResponse(item, null);
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
        userAccessGuard.assertWritable(sellerId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));

        validateOwner(sellerId, item);

        if (STATUS_SOLD.equals(item.getStatus())) {
            throw new BusinessException("已售出商品不可编辑");
        }

        if (STATUS_OFF_SHELF.equals(item.getStatus())) {
            throw new BusinessException("已下架商品不可编辑");
        }
        if (STATUS_EXPIRED.equals(item.getStatus())) {
            throw new BusinessException("已过期商品不可编辑");
        }

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategoryId(request.getCategoryId());
        item.setCategoryName(request.getCategoryName());
        item.setPrice(request.getPrice());
        item.setConditionStar(request.getConditionStar());
        item.setTradeMode(normalizeTradeMode(request.getTradeMode()));
        item.setExpireAt(request.getExpireAt());
        List<String> images = normalizeImages(request.getImages());
        item.setImages(images);
        item.setCoverImage(resolveCoverImage(images));
        item.setLocation(normalizeLocation(request.getLocation()));
        item.setUpdatedAt(new Date());

        Item saved = itemRepository.save(item);
        itemCacheInvalidationService.evictItem(itemId);
        return toDetailResponse(saved, null);
    }

    @Override
    public void offShelfItem(String sellerId, String itemId) {
        userAccessGuard.assertWritable(sellerId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));

        validateOwner(sellerId, item);

        if (STATUS_OFF_SHELF.equals(item.getStatus())) {
            throw new BusinessException("商品已下架");
        }

        if (STATUS_SOLD.equals(item.getStatus())) {
            throw new BusinessException("已售出商品不可下架");
        }

        item.setStatus(STATUS_OFF_SHELF);
        item.setUpdatedAt(new Date());
        itemRepository.save(item);
        itemCacheInvalidationService.evictItem(itemId);
    }

    @Override
    @Cacheable(cacheNames = "item:search")
    public SearchItemPageResponse searchItems(String q,
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
                                              Integer pageSize) {
        int safePage = (page == null || page < 1) ? 1 : page;
        int safePageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("status").is(STATUS_ON_SALE));

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

        RequestLocation requestLocation = normalizeRequestLocation(lat, lng);
        Map<String, User> sellerCache = new HashMap<>();
        List<ItemDistanceView> processedItems = mongoTemplate.find(query, Item.class).stream()
                .map(item -> buildItemDistanceView(item, requestLocation, sellerCache))
                .filter(view -> matchesRadius(view, radiusMeters, requestLocation))
                .sorted(buildSearchComparator(sort, sortBy, requestLocation))
                .toList();

        int fromIndex = Math.min((safePage - 1) * safePageSize, processedItems.size());
        int toIndex = Math.min(fromIndex + safePageSize, processedItems.size());
        List<SearchItemResponse> list = processedItems.subList(fromIndex, toIndex).stream()
                .map(view -> toSearchResponse(view, sellerCache))
                .toList();

        return new SearchItemPageResponse(list, processedItems.size());
    }

    @Override
    @Cacheable(cacheNames = "item:recommend")
    public SearchItemPageResponse recommendItems(Double lat,
                                                 Double lng,
                                                 Integer radiusMeters,
                                                 String sortBy,
                                                 Integer page,
                                                 Integer pageSize) {
        int safePage = (page == null || page < 1) ? 1 : page;
        int safePageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(STATUS_ON_SALE));

        RequestLocation requestLocation = normalizeRequestLocation(lat, lng);
        Map<String, User> sellerCache = new HashMap<>();
        List<ItemDistanceView> processedItems = mongoTemplate.find(query, Item.class).stream()
                .map(item -> buildItemDistanceView(item, requestLocation, sellerCache))
                .filter(view -> matchesRadius(view, radiusMeters, requestLocation))
                .sorted(buildRecommendComparator(sortBy, requestLocation))
                .toList();

        int fromIndex = Math.min((safePage - 1) * safePageSize, processedItems.size());
        int toIndex = Math.min(fromIndex + safePageSize, processedItems.size());
        List<SearchItemResponse> list = processedItems.subList(fromIndex, toIndex).stream()
                .map(view -> toSearchResponse(view, sellerCache))
                .toList();

        return new SearchItemPageResponse(list, processedItems.size());
    }

    @Override
    public void addItemComment(String userId, String itemId, String content, Integer rating) {
        userAccessGuard.assertWritable(userId);
        itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));

        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BusinessException("评论评分必须在1到5之间");
        }

        ItemComment itemComment = new ItemComment();
        itemComment.setItemId(itemId);
        itemComment.setUserId(userId);
        itemComment.setUsername(resolveDisplayName(userId));
        itemComment.setComment(content);
        itemComment.setRating(rating);
        itemComment.setCreatedAt(new Date());

        itemCommentRepository.save(itemComment);
    }

    @Override
    public List<ItemCommentResponse> getItemComments(String itemId) {
        itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));

        return itemCommentRepository.findByItemId(itemId)
                .stream()
                .map(this::toItemCommentResponse)
                .toList();
    }

    private void validateOwner(String sellerId, Item item) {
        if (!sellerId.equals(item.getSellerId())) {
            throw new BusinessException(403, "无权限操作该商品");
        }
    }

    private ItemDetailResponse toDetailResponse(Item item, Double distanceMeters) {
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
        response.setCoverImage(resolveCoverImage(item.getId(), item.getImages(), item.getCoverImage()));
        response.setTradeMode(item.getTradeMode());
        response.setStatus(item.getStatus());
        response.setCreatedAt(item.getCreatedAt());
        response.setSeller(buildSellerInfo(item.getSellerId()));
        response.setStats(buildStatsInfo(item.getStats()));
        applyLocation(response, item.getLocation(), distanceMeters);
        return response;
    }

    private ItemListResponse toListResponse(Item item) {
        ItemListResponse response = new ItemListResponse();
        response.setItemId(item.getId());
        response.setTitle(item.getTitle());
        response.setPrice(item.getPrice());
        response.setConditionStar(item.getConditionStar());
        response.setImages(item.getImages());
        response.setCoverImage(resolveCoverImage(item.getId(), item.getImages(), item.getCoverImage()));
        response.setStatus(item.getStatus());
        response.setLocation(toLocationDto(item.getLocation()));
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }

    private SearchItemResponse toSearchResponse(ItemDistanceView view, Map<String, User> sellerCache) {
        Item item = view.getItem();
        SearchItemResponse response = new SearchItemResponse();
        response.setItemId(item.getId());
        response.setTitle(item.getTitle());
        response.setPrice(item.getPrice());
        response.setConditionStar(item.getConditionStar());
        response.setCoverImage(resolveCoverImage(item.getId(), item.getImages(), item.getCoverImage()));
        response.setHotScore(resolveHotScore(item));
        response.setSeller(buildSearchSellerInfo(item.getSellerId(), sellerCache));
        response.setLocation(toLocationDto(item.getLocation()));
        response.setDistanceMeters(view.getDistanceMeters());
        return response;
    }

    private void applyLocation(ItemDetailResponse response, Item.Location location, Double distanceMeters) {
        LocationDTO locationDTO = toLocationDto(location);
        response.setLocation(locationDTO);
        response.setDistanceMeters(distanceMeters);

        if (locationDTO != null) {
            response.setLat(locationDTO.getLat());
            response.setLng(locationDTO.getLng());
        } else {
            response.setLat(null);
            response.setLng(null);
        }
    }

    private LocationDTO toLocationDto(Item.Location location) {
        if (location == null) {
            return null;
        }

        LocationDTO response = new LocationDTO();
        response.setLat(location.getLat());
        response.setLng(location.getLng());
        response.setAddress(location.getAddress());
        response.setPoiName(location.getPoiName());
        response.setCoordType(location.getCoordType());
        response.setSource(location.getSource());
        return response;
    }

    private Item.Location normalizeLocation(LocationDTO requestLocation) {
        if (requestLocation == null) {
            return null;
        }

        Double lat = requestLocation.getLat();
        Double lng = requestLocation.getLng();
        boolean hasLat = lat != null;
        boolean hasLng = lng != null;
        if (hasLat != hasLng) {
            throw new BusinessException("location经纬度必须同时提供");
        }

        String address = trimToNull(requestLocation.getAddress());
        String poiName = trimToNull(requestLocation.getPoiName());
        boolean hasCoordinates = hasLat;
        if (!hasCoordinates && address == null && poiName == null) {
            return null;
        }

        Item.Location location = new Item.Location();
        if (hasCoordinates) {
            validateCoordinates(lat, lng, "location坐标");
            String coordType = normalizeCoordType(requestLocation.getCoordType(), COORD_TYPE_GCJ02);
            if (COORD_TYPE_WGS84.equals(coordType)) {
                double[] gcjCoordinates = CoordinateTransformUtil.wgs84ToGcj02(lat, lng);
                lat = gcjCoordinates[0];
                lng = gcjCoordinates[1];
                coordType = COORD_TYPE_GCJ02;
            }
            location.setLat(lat);
            location.setLng(lng);
            location.setCoordType(coordType);
        }

        location.setAddress(address);
        location.setPoiName(poiName);
        location.setSource(normalizeSource(requestLocation.getSource()));
        return location;
    }

    private RequestLocation normalizeRequestLocation(Double lat, Double lng) {
        boolean hasLat = lat != null;
        boolean hasLng = lng != null;
        if (!hasLat && !hasLng) {
            return null;
        }
        if (hasLat != hasLng) {
            throw new BusinessException("请求定位坐标必须同时提供");
        }

        validateCoordinates(lat, lng, "请求定位坐标");
        double[] gcjCoordinates = CoordinateTransformUtil.wgs84ToGcj02(lat, lng);
        return new RequestLocation(gcjCoordinates[0], gcjCoordinates[1]);
    }

    private String normalizeCoordType(String coordType, String defaultCoordType) {
        if (coordType == null || coordType.isBlank()) {
            return defaultCoordType;
        }

        String normalized = coordType.trim().toUpperCase(Locale.ROOT);
        if (!COORD_TYPE_WGS84.equals(normalized) && !COORD_TYPE_GCJ02.equals(normalized)) {
            throw new BusinessException("不支持的坐标类型: " + coordType);
        }
        return normalized;
    }

    private String normalizeSource(String source) {
        String normalized = trimToNull(source);
        if (normalized == null) {
            return DEFAULT_LOCATION_SOURCE;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeTradeMode(String tradeMode) {
        String normalized = trimToNull(tradeMode);
        if (normalized == null) {
            throw new BusinessException("tradeMode不能为空");
        }

        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!TRADE_MODE_ONLINE.equals(normalized)
                && !TRADE_MODE_OFFLINE.equals(normalized)
                && !TRADE_MODE_BOTH.equals(normalized)) {
            throw new BusinessException("tradeMode仅支持ONLINE、OFFLINE或BOTH");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateCoordinates(Double lat, Double lng, String fieldName) {
        if (!isValidCoordinate(lat, lng)) {
            if (lat == null || lng == null) {
                throw new BusinessException(fieldName + "不能为空");
            }
            throw new BusinessException(fieldName + "超出合法范围");
        }
    }

    private boolean isValidCoordinate(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return false;
        }
        if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
            return false;
        }
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    private ItemDistanceView buildItemDistanceView(Item item,
                                                   RequestLocation requestLocation,
                                                   Map<String, User> sellerCache) {
        User seller = findSeller(item.getSellerId(), sellerCache);
        Double distanceMeters = calculateDistanceMeters(item, requestLocation);
        int recommendScore = buildRecommendScore(item, seller);
        return new ItemDistanceView(item, distanceMeters, recommendScore);
    }

    private Double calculateDistanceMeters(Item item, RequestLocation requestLocation) {
        if (requestLocation == null) {
            return null;
        }

        Item.Location location = item.getLocation();
        if (location == null) {
            log.warn("Skip distance calculation for item {} because location is missing", item.getId());
            return null;
        }

        Double itemLat = location.getLat();
        Double itemLng = location.getLng();
        if (!isValidCoordinate(itemLat, itemLng)) {
            log.warn("Skip distance calculation for item {} because coordinates are invalid: lat={}, lng={}",
                    item.getId(), itemLat, itemLng);
            return null;
        }

        return DistanceUtil.haversineMeters(
                requestLocation.lat(),
                requestLocation.lng(),
                itemLat,
                itemLng
        );
    }

    private boolean matchesRadius(ItemDistanceView view, Integer radiusMeters, RequestLocation requestLocation) {
        if (requestLocation == null || radiusMeters == null || radiusMeters <= 0) {
            return true;
        }
        return view.getDistanceMeters() != null && view.getDistanceMeters() <= radiusMeters;
    }

    private Comparator<ItemDistanceView> buildSearchComparator(String sort, String sortBy, RequestLocation requestLocation) {
        if (isDistanceSort(sortBy) && requestLocation != null) {
            return buildDistanceComparator();
        }

        if ("PRICE".equalsIgnoreCase(sort)) {
            return Comparator.comparing((ItemDistanceView view) -> view.getItem().getPrice(), Comparator.nullsLast(BigDecimal::compareTo))
                    .thenComparing(ItemDistanceView::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        if ("HOT".equalsIgnoreCase(sort)) {
            return Comparator.comparingInt(ItemDistanceView::getHotScore)
                    .reversed()
                    .thenComparing(ItemDistanceView::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        return Comparator.comparing(ItemDistanceView::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<ItemDistanceView> buildRecommendComparator(String sortBy, RequestLocation requestLocation) {
        if (isDistanceSort(sortBy) && requestLocation != null) {
            return buildDistanceComparator();
        }

        return Comparator.comparingInt(ItemDistanceView::getRecommendScore)
                .reversed()
                .thenComparing(ItemDistanceView::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<ItemDistanceView> buildDistanceComparator() {
        return Comparator.comparing(ItemDistanceView::getDistanceMeters, Comparator.nullsLast(Double::compareTo))
                .thenComparing(ItemDistanceView::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private boolean isDistanceSort(String sortBy) {
        return sortBy != null && SORT_BY_DISTANCE.equalsIgnoreCase(sortBy.trim());
    }

    private List<String> normalizeImages(List<String> images) {
        List<String> normalized = images == null ? new ArrayList<>() : new ArrayList<>(images);
        if (normalized.size() > 9) {
            throw new BusinessException("商品图片最多9张");
        }
        return normalized;
    }

    private String resolveCoverImage(List<?> images) {
        return resolveCoverImage(null, images, null);
    }

    private String resolveCoverImage(String itemId, List<?> images, String existingCoverImage) {
        String extractedImageUrl = extractImageUrl(itemId, images);
        if (extractedImageUrl != null) {
            return extractedImageUrl;
        }
        return trimToNull(existingCoverImage);
    }

    private String extractImageUrl(String itemId, List<?> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }

        for (int i = 0; i < images.size(); i++) {
            Object image = images.get(i);
            if (image instanceof String imageUrl) {
                String normalized = trimToNull(imageUrl);
                if (normalized != null) {
                    return normalized;
                }
                log.warn("Skip blank item image entry for item {} at index {}", itemId, i);
                continue;
            }
            if (image instanceof Map<?, ?> imageMap) {
                Object url = imageMap.get(IMAGE_URL_FIELD);
                if (url instanceof String imageUrl) {
                    String normalized = trimToNull(imageUrl);
                    if (normalized != null) {
                        return normalized;
                    }
                }
                log.warn("Skip malformed item image entry for item {} at index {} because url is missing or invalid: image={}",
                        itemId, i, imageMap);
                continue;
            }

            log.warn("Skip malformed item image entry for item {} at index {} because type is unsupported: type={}",
                    itemId, i, image == null ? null : image.getClass().getName());
        }
        return null;
    }

    private ItemDetailResponse.SellerInfo buildSellerInfo(String sellerId) {
        ItemDetailResponse.SellerInfo sellerInfo = new ItemDetailResponse.SellerInfo();
        sellerInfo.setUserId(sellerId);

        userRepository.findById(sellerId).ifPresent(user -> {
            sellerInfo.setNickname(resolveDisplayName(user));
            sellerInfo.setAvatarUrl(user.getAvatarUrl());
            sellerInfo.setCreditScore(resolveCreditScore(user));
            sellerInfo.setCreditLevel(resolveCreditLevel(user));
            sellerInfo.setReviewCount(resolveReviewCount(user));
            sellerInfo.setAverageRating(resolveAverageRating(user));
        });

        if (sellerInfo.getCreditScore() == null) {
            sellerInfo.setCreditScore(DEFAULT_CREDIT_SCORE);
            sellerInfo.setCreditLevel(DEFAULT_CREDIT_LEVEL);
            sellerInfo.setReviewCount(DEFAULT_REVIEW_COUNT);
            sellerInfo.setAverageRating(DEFAULT_AVERAGE_RATING);
        }

        return sellerInfo;
    }

    private ItemDetailResponse.StatsInfo buildStatsInfo(Item.Stats stats) {
        ItemDetailResponse.StatsInfo statsInfo = new ItemDetailResponse.StatsInfo();
        statsInfo.setViewCount(stats == null || stats.getViewCount() == null ? 0 : stats.getViewCount());
        statsInfo.setFavoriteCount(stats == null || stats.getFavoriteCount() == null ? 0 : stats.getFavoriteCount());
        return statsInfo;
    }

    private ItemCommentResponse toItemCommentResponse(ItemComment itemComment) {
        ItemCommentResponse response = new ItemCommentResponse();
        response.setCommentId(itemComment.getId());
        response.setUserId(itemComment.getUserId());
        response.setNickname(itemComment.getUsername());
        response.setContent(itemComment.getComment());
        response.setCreatedAt(itemComment.getCreatedAt());
        return response;
    }

    private String resolveDisplayName(String userId) {
        return userRepository.findById(userId)
                .map(this::resolveDisplayName)
                .orElse(userId);
    }

    private String resolveDisplayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getId();
    }

    private SearchItemResponse.SellerInfo buildSearchSellerInfo(String sellerId, Map<String, User> sellerCache) {
        SearchItemResponse.SellerInfo sellerInfo = new SearchItemResponse.SellerInfo();
        sellerInfo.setUserId(sellerId);

        User seller = findSeller(sellerId, sellerCache);
        if (seller == null) {
            sellerInfo.setCreditScore(DEFAULT_CREDIT_SCORE);
            sellerInfo.setCreditLevel(DEFAULT_CREDIT_LEVEL);
            sellerInfo.setReviewCount(DEFAULT_REVIEW_COUNT);
            sellerInfo.setAverageRating(DEFAULT_AVERAGE_RATING);
            return sellerInfo;
        }

        sellerInfo.setNickname(resolveDisplayName(seller));
        sellerInfo.setAvatarUrl(seller.getAvatarUrl());
        sellerInfo.setCreditScore(resolveCreditScore(seller));
        sellerInfo.setCreditLevel(resolveCreditLevel(seller));
        sellerInfo.setReviewCount(resolveReviewCount(seller));
        sellerInfo.setAverageRating(resolveAverageRating(seller));
        return sellerInfo;
    }

    private User findSeller(String sellerId, Map<String, User> sellerCache) {
        if (sellerCache.containsKey(sellerId)) {
            return sellerCache.get(sellerId);
        }

        User seller = userRepository.findById(sellerId).orElse(null);
        sellerCache.put(sellerId, seller);
        return seller;
    }

    private int resolveHotScore(Item item) {
        int hotScore = 0;
        if (item.getStats() != null) {
            hotScore += item.getStats().getViewCount() == null ? 0 : item.getStats().getViewCount();
            hotScore += item.getStats().getFavoriteCount() == null ? 0 : item.getStats().getFavoriteCount();
            hotScore += item.getStats().getChatCount() == null ? 0 : item.getStats().getChatCount();
        }
        return hotScore;
    }

    private int buildRecommendScore(Item item, User seller) {
        int hotComponent = Math.min(100, resolveHotScore(item));
        int creditScore = seller == null ? DEFAULT_CREDIT_SCORE : resolveCreditScore(seller);
        return (int) Math.round(hotComponent * 0.7 + creditScore * 0.3);
    }

    private Integer resolveCreditScore(User user) {
        return user.getCreditScore() == null ? DEFAULT_CREDIT_SCORE : user.getCreditScore();
    }

    private String resolveCreditLevel(User user) {
        if (user.getCreditLevel() == null || user.getCreditLevel().isBlank()) {
            return DEFAULT_CREDIT_LEVEL;
        }
        return user.getCreditLevel();
    }

    private Integer resolveReviewCount(User user) {
        return user.getReviewCount() == null ? DEFAULT_REVIEW_COUNT : user.getReviewCount();
    }

    private Double resolveAverageRating(User user) {
        return user.getAverageRating() == null ? DEFAULT_AVERAGE_RATING : user.getAverageRating();
    }

    private record RequestLocation(double lat, double lng) {
    }

    private static class ItemDistanceView {
        private final Item item;
        private final Double distanceMeters;
        private final int recommendScore;

        private ItemDistanceView(Item item, Double distanceMeters, int recommendScore) {
            this.item = item;
            this.distanceMeters = distanceMeters;
            this.recommendScore = recommendScore;
        }

        public Item getItem() {
            return item;
        }

        public Double getDistanceMeters() {
            return distanceMeters;
        }

        public int getRecommendScore() {
            return recommendScore;
        }

        public int getHotScore() {
            if (item == null || item.getStats() == null) {
                return 0;
            }
            int viewCount = item.getStats().getViewCount() == null ? 0 : item.getStats().getViewCount();
            int favoriteCount = item.getStats().getFavoriteCount() == null ? 0 : item.getStats().getFavoriteCount();
            int chatCount = item.getStats().getChatCount() == null ? 0 : item.getStats().getChatCount();
            return viewCount + favoriteCount + chatCount;
        }

        public Date getCreatedAt() {
            return item == null ? null : item.getCreatedAt();
        }
    }
}
