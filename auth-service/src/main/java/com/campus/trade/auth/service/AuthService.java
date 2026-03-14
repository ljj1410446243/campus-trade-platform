package com.campus.trade.auth.service;

import com.campus.trade.auth.dto.*;

/**
 * 认证服务接口
 */
public interface AuthService {

  String ping();

  LoginResponse login(LoginRequest request);

  TokenPayloadResponse parseToken(String token);

  void register(RegisterRequest request);

  void campusVerify(String userId, CampusVerifyRequest request);
}
