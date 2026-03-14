package com.campus.trade.user.service;

import com.campus.trade.user.dto.AddBrowseHistoryRequest;
import com.campus.trade.user.dto.AddFavoriteRequest;
import com.campus.trade.user.dto.BrowseHistoryItemResponse;
import com.campus.trade.user.dto.FavoriteItemResponse;
import com.campus.trade.user.dto.UpdateUserProfileRequest;
import com.campus.trade.user.dto.UserMeResponse;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

  UserMeResponse getMe(String userId);

  UserMeResponse updateMe(String userId, UpdateUserProfileRequest request);

  void addFavorite(String userId, AddFavoriteRequest request);

  void removeFavorite(String userId, String itemId);

  List<FavoriteItemResponse> listFavorites(String userId);

  void addBrowseHistory(String userId, AddBrowseHistoryRequest request);

  List<BrowseHistoryItemResponse> listBrowseHistory(String userId);
}
