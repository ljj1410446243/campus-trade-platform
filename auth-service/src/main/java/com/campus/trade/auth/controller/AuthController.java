package com.campus.trade.auth.controller;

import com.campus.trade.auth.dto.*;
import com.campus.trade.auth.exception.AuthenticationException;
import com.campus.trade.auth.service.AuthService;
import com.campus.trade.auth.util.RefreshTokenCookieManager;
import com.campus.trade.auth.util.RequestContextExtractor;
import com.campus.trade.auth.util.JwtUtil;
import com.campus.trade.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
  private final RefreshTokenCookieManager refreshTokenCookieManager;
  private final RequestContextExtractor requestContextExtractor;

  public AuthController(AuthService authService,
                        JwtUtil jwtUtil,
                        RefreshTokenCookieManager refreshTokenCookieManager,
                        RequestContextExtractor requestContextExtractor) {
    this.authService = authService;
    this.jwtUtil = jwtUtil;
    this.refreshTokenCookieManager = refreshTokenCookieManager;
    this.requestContextExtractor = requestContextExtractor;
  }

  @GetMapping("/ping")
  public ApiResponse<String> ping() {
    return ApiResponse.success(authService.ping());
  }

  /**
   * 登录接口
   */
  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                          HttpServletRequest httpServletRequest,
                                          HttpServletResponse httpServletResponse) {
    var result = authService.login(request, requestContextExtractor.extract(httpServletRequest));
    refreshTokenCookieManager.writeCookie(httpServletResponse, result.refreshToken());
    return ApiResponse.success(result.loginResponse());
  }

  @PostMapping("/refresh")
  public ApiResponse<LoginResponse> refresh(HttpServletRequest httpServletRequest,
                                            HttpServletResponse httpServletResponse) {
    String refreshToken = refreshTokenCookieManager.readCookie(httpServletRequest);
    try {
      var result = authService.refresh(refreshToken, requestContextExtractor.extract(httpServletRequest));
      refreshTokenCookieManager.writeCookie(httpServletResponse, result.refreshToken());
      return ApiResponse.success(result.loginResponse());
    } catch (AuthenticationException e) {
      refreshTokenCookieManager.clearCookie(httpServletResponse);
      throw e;
    }
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(HttpServletRequest httpServletRequest,
                                  HttpServletResponse httpServletResponse) {
    String refreshToken = refreshTokenCookieManager.readCookie(httpServletRequest);
    authService.logout(refreshToken);
    refreshTokenCookieManager.clearCookie(httpServletResponse);
    return ApiResponse.success();
  }

  /**
   * 解析 Token 接口
   */
  @GetMapping("/token/parse")
  public ApiResponse<TokenPayloadResponse> parseToken(
          @RequestHeader("Authorization") String authorizationHeader) {

    String token = authorizationHeader.replace("Bearer ", "");
    jwtUtil.validateAccessToken(token);
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
    jwtUtil.validateAccessToken(token);

    String userId = jwtUtil.getUserId(token);

    authService.campusVerify(userId, request);

    return ApiResponse.success();
  }
}
