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
import com.campus.trade.user.repository.BrowseHistoryRepository;
import com.campus.trade.user.dto.BrowseHistoryItemResponse;
import com.campus.trade.user.exception.BusinessException;
import java.util.List;
/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final BrowseHistoryRepository browseHistoryRepository;

    public UserServiceImpl(UserRepository userRepository,
                           FavoriteRepository favoriteRepository,
                           BrowseHistoryRepository browseHistoryRepository) {
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.browseHistoryRepository = browseHistoryRepository;
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
                user.getNickname(),
                user.getAvatarUrl(),
                user.isCampusVerified(),
                95,
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
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(favorite -> new FavoriteItemResponse(
                        favorite.getItemId(),
                        favorite.getCreatedAt()
                ))
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
        return browseHistoryRepository.findByUserIdOrderByViewedAtDesc(userId)
                .stream()
                .map(history -> new BrowseHistoryItemResponse(
                        history.getItemId(),
                        history.getViewedAt(),
                        history.getSource()
                ))
                .toList();
    }
}
