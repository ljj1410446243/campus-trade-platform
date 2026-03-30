package com.campus.trade.auth.service;

import com.campus.trade.auth.dto.*;

/**
 * 认证服务接口
 */
public interface AuthService {

  String ping();

  AuthTokenResult login(LoginRequest request, ClientContext clientContext);

  AuthTokenResult refresh(String refreshToken, ClientContext clientContext);

  void logout(String refreshToken);

  TokenPayloadResponse parseToken(String token);

  void register(RegisterRequest request);

  void campusVerify(String userId, CampusVerifyRequest request);
}
