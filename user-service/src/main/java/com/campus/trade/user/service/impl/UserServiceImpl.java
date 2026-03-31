package com.campus.trade.user.service.impl;

import com.campus.trade.user.dto.AddFavoriteRequest;
import com.campus.trade.user.dto.UpdateUserProfileRequest;
import com.campus.trade.user.dto.UserMeResponse;
import com.campus.trade.user.model.Favorite;
import com.campus.trade.user.model.User;
import com.campus.trade.user.repository.FavoriteRepository;
import com.campus.trade.user.repository.UserRepository;
import com.campus.trade.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Date;
import com.campus.trade.user.dto.FavoriteItemResponse;
import java.util.List;
import com.campus.trade.user.dto.AddBrowseHistoryRequest;
import com.campus.trade.user.model.BrowseHistory;
import com.campus.trade.user.model.Item;
import com.campus.trade.user.repository.BrowseHistoryRepository;
import com.campus.trade.user.repository.ItemRepository;
import com.campus.trade.user.dto.BrowseHistoryItemResponse;
import com.campus.trade.user.exception.BusinessException;
import java.util.HashMap;
import java.util.Map;
/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    private static final int DEFAULT_CREDIT_SCORE = 0;
    private static final int DEFAULT_REVIEW_COUNT = 0;
    private static final double DEFAULT_AVERAGE_RATING = 0D;
    private static final String DEFAULT_CREDIT_LEVEL = "NEW";

    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final BrowseHistoryRepository browseHistoryRepository;
    private final ItemRepository itemRepository;

    public UserServiceImpl(UserRepository userRepository,
                           FavoriteRepository favoriteRepository,
                           BrowseHistoryRepository browseHistoryRepository,
                           ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.browseHistoryRepository = browseHistoryRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public UserMeResponse getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        return buildUserMeResponse(user);
    }

    @Override
    public UserMeResponse updateMe(String userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setNickname(request.getNickname());
        user.setAvatarUrl(request.getAvatarUrl());

        User savedUser = userRepository.save(user);

        return buildUserMeResponse(savedUser);
    }

    @Override
    public void addFavorite(String userId, AddFavoriteRequest request) {
        boolean exists = favoriteRepository
                .findByUserIdAndItemId(userId, request.getItemId())
                .isPresent();

        if (exists) {
            throw new BusinessException("该商品已收藏");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setItemId(request.getItemId());
        favorite.setCreatedAt(new Date());

        favoriteRepository.save(favorite);
    }

    private UserMeResponse buildUserMeResponse(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.isCampusVerified(),
                resolveCreditScore(user),
                resolveCreditLevel(user),
                resolveReviewCount(user),
                resolveAverageRating(user),
                user.getRole()
        );
    }

    @Override
    public void removeFavorite(String userId, String itemId) {
        boolean exists = favoriteRepository.findByUserIdAndItemId(userId, itemId).isPresent();

        if (!exists) {
            throw new BusinessException("收藏记录不存在");
        }

        favoriteRepository.deleteByUserIdAndItemId(userId, itemId);
    }

    @Override
    public List<FavoriteItemResponse> listFavorites(String userId) {
        Map<String, Item> itemCache = new HashMap<>();
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(favorite -> toFavoriteItemResponse(favorite, itemCache))
                .toList();
    }

    @Override
    public void addBrowseHistory(String userId, AddBrowseHistoryRequest request) {
        BrowseHistory history = new BrowseHistory();
        history.setUserId(userId);
        history.setItemId(request.getItemId());
        history.setViewedAt(new Date());

        String source = request.getSource();
        history.setSource(source == null || source.isBlank() ? "DIRECT" : source);

        browseHistoryRepository.save(history);
    }

    @Override
    public List<BrowseHistoryItemResponse> listBrowseHistory(String userId) {
        Map<String, Item> itemCache = new HashMap<>();
        return browseHistoryRepository.findByUserIdOrderByViewedAtDesc(userId)
                .stream()
                .map(history -> toBrowseHistoryItemResponse(history, itemCache))
                .toList();
    }

    private FavoriteItemResponse toFavoriteItemResponse(Favorite favorite, Map<String, Item> itemCache) {
        Item item = findItem(favorite.getItemId(), itemCache);
        return new FavoriteItemResponse(
                favorite.getItemId(),
                item == null ? null : item.getTitle(),
                item == null ? null : item.getPrice(),
                item == null ? null : item.getConditionStar(),
                item == null ? null : item.getCoverImage(),
                favorite.getCreatedAt()
        );
    }

    private BrowseHistoryItemResponse toBrowseHistoryItemResponse(BrowseHistory history, Map<String, Item> itemCache) {
        Item item = findItem(history.getItemId(), itemCache);
        return new BrowseHistoryItemResponse(
                history.getItemId(),
                item == null ? null : item.getTitle(),
                item == null ? null : item.getPrice(),
                history.getViewedAt(),
                history.getSource()
        );
    }

    private Item findItem(String itemId, Map<String, Item> itemCache) {
        if (itemCache.containsKey(itemId)) {
            return itemCache.get(itemId);
        }

        Item item = itemRepository.findById(itemId).orElse(null);
        itemCache.put(itemId, item);
        return item;
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
}
