package com.campus.trade.user.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.user.dto.UpdateUserProfileRequest;
import com.campus.trade.user.dto.UserMeResponse;
import com.campus.trade.user.service.UserService;
import com.campus.trade.user.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.campus.trade.user.dto.AddFavoriteRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.campus.trade.user.dto.FavoriteItemResponse;
import java.util.List;
import com.campus.trade.user.dto.AddBrowseHistoryRequest;
import org.springframework.web.bind.annotation.PostMapping;

import com.campus.trade.user.dto.BrowseHistoryItemResponse;
import java.util.List;
/**
 * User 服务控制器
 */
@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;
  private final JwtUtil jwtUtil;

  public UserController(UserService userService, JwtUtil jwtUtil) {
    this.userService = userService;
    this.jwtUtil = jwtUtil;
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

    String token = authorizationHeader.replace("Bearer ", "");
    String userId = jwtUtil.getUserId(token);

    return ApiResponse.success(userService.getMe(userId));
  }

  /**
   * 更新当前用户资料
   */
  @PutMapping("/me")
  public ApiResponse<UserMeResponse> updateMe(
          @RequestHeader("Authorization") String authorizationHeader,
          @Valid @RequestBody UpdateUserProfileRequest request) {

    String token = authorizationHeader.replace("Bearer ", "");
    String userId = jwtUtil.getUserId(token);

    return ApiResponse.success(userService.updateMe(userId, request));
  }

  @PostMapping("/me/favorites")
  public ApiResponse<Void> addFavorite(
          @RequestHeader("Authorization") String authorizationHeader,
          @Valid @RequestBody AddFavoriteRequest request) {

    String token = authorizationHeader.replace("Bearer ", "");
    String userId = jwtUtil.getUserId(token);

    userService.addFavorite(userId, request);

    return ApiResponse.success();
  }

  @DeleteMapping("/me/favorites/{itemId}")
  public ApiResponse<Void> removeFavorite(
          @RequestHeader("Authorization") String authorizationHeader,
          @PathVariable String itemId) {

    String token = authorizationHeader.replace("Bearer ", "");
    String userId = jwtUtil.getUserId(token);

    userService.removeFavorite(userId, itemId);

    return ApiResponse.success();
  }

  @GetMapping("/me/favorites")
  public ApiResponse<List<FavoriteItemResponse>> listFavorites(
          @RequestHeader("Authorization") String authorizationHeader) {

    String token = authorizationHeader.replace("Bearer ", "");
    String userId = jwtUtil.getUserId(token);

    return ApiResponse.success(userService.listFavorites(userId));
  }

  @PostMapping("/me/browse-history")
  public ApiResponse<Void> addBrowseHistory(
          @RequestHeader("Authorization") String authorizationHeader,
          @Valid @RequestBody AddBrowseHistoryRequest request) {

    String token = authorizationHeader.replace("Bearer ", "");
    String userId = jwtUtil.getUserId(token);

    userService.addBrowseHistory(userId, request);

    return ApiResponse.success();
  }

  @GetMapping("/me/browse-history")
  public ApiResponse<List<BrowseHistoryItemResponse>> listBrowseHistory(
          @RequestHeader("Authorization") String authorizationHeader) {

    String token = authorizationHeader.replace("Bearer ", "");
    String userId = jwtUtil.getUserId(token);

    return ApiResponse.success(userService.listBrowseHistory(userId));
  }
}
