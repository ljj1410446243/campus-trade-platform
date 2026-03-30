package com.campus.trade.user.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.user.dto.AddBrowseHistoryRequest;
import com.campus.trade.user.dto.AddFavoriteRequest;
import com.campus.trade.user.dto.BrowseHistoryItemResponse;
import com.campus.trade.user.dto.FavoriteItemResponse;
import com.campus.trade.user.dto.UpdateUserProfileRequest;
import com.campus.trade.user.dto.UserMeResponse;
import com.campus.trade.user.service.UserService;
import com.campus.trade.user.util.LoginUserHelper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User 服务控制器
 */
@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;
  private final LoginUserHelper loginUserHelper;

  public UserController(UserService userService, LoginUserHelper loginUserHelper) {
    this.userService = userService;
    this.loginUserHelper = loginUserHelper;
  }

  /**
   * 服务健康检查接口
   */
  @GetMapping("/ping")
  public ApiResponse<String> ping() {
    return ApiResponse.success("user-service is running");
  }

  /**
   * 获取当前用户信息
   */
  @GetMapping("/me")
  public ApiResponse<UserMeResponse> getMe(
          @RequestHeader("Authorization") String authorizationHeader) {

    return ApiResponse.success(userService.getMe(loginUserHelper.getCurrentUserId(authorizationHeader)));
  }

  /**
   * 更新当前用户资料
   */
  @PutMapping("/me")
  public ApiResponse<UserMeResponse> updateMe(
          @RequestHeader("Authorization") String authorizationHeader,
          @Valid @RequestBody UpdateUserProfileRequest request) {

    return ApiResponse.success(userService.updateMe(loginUserHelper.getCurrentUserId(authorizationHeader), request));
  }

  @PostMapping("/me/favorites")
  public ApiResponse<Void> addFavorite(
          @RequestHeader("Authorization") String authorizationHeader,
          @Valid @RequestBody AddFavoriteRequest request) {

    userService.addFavorite(loginUserHelper.getCurrentUserId(authorizationHeader), request);
    return ApiResponse.success();
  }

  @DeleteMapping("/me/favorites/{itemId}")
  public ApiResponse<Void> removeFavorite(
          @RequestHeader("Authorization") String authorizationHeader,
          @PathVariable String itemId) {

    userService.removeFavorite(loginUserHelper.getCurrentUserId(authorizationHeader), itemId);
    return ApiResponse.success();
  }

  @GetMapping("/me/favorites")
  public ApiResponse<List<FavoriteItemResponse>> listFavorites(
          @RequestHeader("Authorization") String authorizationHeader) {

    return ApiResponse.success(userService.listFavorites(loginUserHelper.getCurrentUserId(authorizationHeader)));
  }

  @PostMapping("/me/browse-history")
  public ApiResponse<Void> addBrowseHistory(
          @RequestHeader("Authorization") String authorizationHeader,
          @Valid @RequestBody AddBrowseHistoryRequest request) {

    userService.addBrowseHistory(loginUserHelper.getCurrentUserId(authorizationHeader), request);
    return ApiResponse.success();
  }

  @GetMapping("/me/browse-history")
  public ApiResponse<List<BrowseHistoryItemResponse>> listBrowseHistory(
          @RequestHeader("Authorization") String authorizationHeader) {

    return ApiResponse.success(userService.listBrowseHistory(loginUserHelper.getCurrentUserId(authorizationHeader)));
  }
}
