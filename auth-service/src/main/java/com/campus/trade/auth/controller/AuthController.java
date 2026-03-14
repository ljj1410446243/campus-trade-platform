package com.campus.trade.auth.controller;

import com.campus.trade.auth.dto.*;
import com.campus.trade.auth.service.AuthService;
import com.campus.trade.auth.util.JwtUtil;
import com.campus.trade.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Auth 控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final JwtUtil jwtUtil;
  private final AuthService authService;

  public AuthController(AuthService authService, JwtUtil jwtUtil) {
    this.authService = authService;
    this.jwtUtil = jwtUtil;
  }

  @GetMapping("/ping")
  public ApiResponse<String> ping() {
    return ApiResponse.success(authService.ping());
  }

  /**
   * 登录接口
   */
  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = authService.login(request);
    return ApiResponse.success(response);
  }

  /**
   * 解析 Token 接口
   */
  @GetMapping("/token/parse")
  public ApiResponse<TokenPayloadResponse> parseToken(
          @RequestHeader("Authorization") String authorizationHeader) {

    String token = authorizationHeader.replace("Bearer ", "");
    TokenPayloadResponse response = authService.parseToken(token);
    return ApiResponse.success(response);
  }

  @PostMapping("/register")
  public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {

    authService.register(request);

    return ApiResponse.success();
  }

  @PostMapping("/campus/verify")
  public ApiResponse<Void> campusVerify(
          @RequestHeader("Authorization") String authorizationHeader,
          @Valid @RequestBody CampusVerifyRequest request) {

    String token = authorizationHeader.replace("Bearer ", "");

    String userId = jwtUtil.getUserId(token);

    authService.campusVerify(userId, request);

    return ApiResponse.success();
  }
}
