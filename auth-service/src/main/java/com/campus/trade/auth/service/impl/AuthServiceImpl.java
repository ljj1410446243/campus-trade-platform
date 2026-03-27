package com.campus.trade.auth.service.impl;

import com.campus.trade.auth.config.RefreshTokenProperties;
import com.campus.trade.auth.constant.ErrorCode;
import com.campus.trade.auth.dto.*;
import com.campus.trade.auth.exception.AuthenticationException;
import com.campus.trade.auth.exception.BusinessException;
import com.campus.trade.auth.model.RefreshSession;
import com.campus.trade.auth.model.User;
import com.campus.trade.auth.repository.RefreshSessionRepository;
import com.campus.trade.auth.repository.UserRepository;
import com.campus.trade.auth.service.AuthTokenResult;
import com.campus.trade.auth.service.AuthService;
import com.campus.trade.auth.service.ClientContext;
import com.campus.trade.auth.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * AuthService实现
 */
@Service
public class AuthServiceImpl implements AuthService {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final RefreshSessionRepository refreshSessionRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final RefreshTokenProperties refreshTokenProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  public AuthServiceImpl(JwtUtil jwtUtil,
                         UserRepository userRepository,
                         RefreshSessionRepository refreshSessionRepository,
                         BCryptPasswordEncoder passwordEncoder,
                         RefreshTokenProperties refreshTokenProperties) {
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
    this.refreshSessionRepository = refreshSessionRepository;
    this.passwordEncoder = passwordEncoder;
    this.refreshTokenProperties = refreshTokenProperties;
  }

  @Override
  public String ping() {
    return "auth-service is running";
  }

  @Override
  public AuthTokenResult login(LoginRequest request, ClientContext clientContext) {

    User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() ->
                    new BusinessException(ErrorCode.AUTH_ERROR, HttpStatus.UNAUTHORIZED, "用户名或密码错误"));

    boolean passwordMatched = passwordEncoder.matches(
            request.getPassword(),
            user.getPasswordHash()
    );

    if (!passwordMatched) {
      throw new BusinessException(ErrorCode.AUTH_ERROR, HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }

    TokenBundle tokenBundle = createSession(user, clientContext);

    return new AuthTokenResult(buildLoginResponse(tokenBundle.accessToken()), tokenBundle.refreshToken());
  }

  @Override
  public AuthTokenResult refresh(String refreshToken, ClientContext clientContext) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new AuthenticationException("refresh token无效或已过期");
    }

    Optional<RefreshSession> sessionOptional = refreshSessionRepository.findByTokenHash(hashToken(refreshToken));
    RefreshSession session = sessionOptional
            .orElseThrow(() -> new AuthenticationException("refresh token无效或已过期"));

    if (session.getRevokedAt() != null || session.getExpiresAt() == null || session.getExpiresAt().isBefore(Instant.now())) {
      session.setRevokedAt(Instant.now());
      refreshSessionRepository.save(session);
      throw new AuthenticationException("refresh token无效或已过期");
    }

    User user = userRepository.findById(session.getUserId())
            .orElseThrow(() -> {
              session.setRevokedAt(Instant.now());
              refreshSessionRepository.save(session);
              return new AuthenticationException("refresh token无效或已过期");
            });

    String newRefreshToken = generateRefreshToken();
    String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), session.getId());
    Instant now = Instant.now();

    session.setTokenHash(hashToken(newRefreshToken));
    session.setLastUsedAt(now);
    session.setExpiresAt(now.plusSeconds(refreshTokenProperties.getExpiration()));
    session.setUserAgent(clientContext.userAgent());
    session.setClientIp(clientContext.clientIp());
    refreshSessionRepository.save(session);

    return new AuthTokenResult(buildLoginResponse(newAccessToken), newRefreshToken);
  }

  @Override
  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }

    refreshSessionRepository.findByTokenHash(hashToken(refreshToken)).ifPresent(session -> {
      session.setRevokedAt(Instant.now());
      refreshSessionRepository.save(session);
    });
  }

  @Override
  public TokenPayloadResponse parseToken(String token) {
    jwtUtil.validateAccessToken(token);
    String userId = jwtUtil.getUserId(token);
    String username = jwtUtil.getUsername(token);
    return new TokenPayloadResponse(userId, username);
  }

  @Override
  public void register(RegisterRequest request) {

    boolean exists = userRepository.findByUsername(request.getUsername()).isPresent();

    if (exists) {
      throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRole("USER");

    userRepository.save(user);
  }

  @Override
  public void campusVerify(String userId, CampusVerifyRequest request) {

    User user = userRepository.findById(userId)
            .orElseThrow(() ->
                    new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

    user.setCampusVerified(true);
    user.setStudentId(request.getStudentId());
    user.setSchoolEmail(request.getSchoolEmail());

    userRepository.save(user);
  }

  private TokenBundle createSession(User user, ClientContext clientContext) {
    String sessionId = UUID.randomUUID().toString();
    String refreshToken = generateRefreshToken();
    Instant now = Instant.now();

    RefreshSession session = new RefreshSession();
    session.setId(sessionId);
    session.setUserId(user.getId());
    session.setTokenHash(hashToken(refreshToken));
    session.setCreatedAt(now);
    session.setLastUsedAt(now);
    session.setExpiresAt(now.plusSeconds(refreshTokenProperties.getExpiration()));
    session.setUserAgent(clientContext.userAgent());
    session.setClientIp(clientContext.clientIp());
    refreshSessionRepository.save(session);

    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), sessionId);
    return new TokenBundle(accessToken, refreshToken);
  }

  private LoginResponse buildLoginResponse(String accessToken) {
    return new LoginResponse(accessToken, jwtUtil.getAccessTokenExpiration(), "Bearer");
  }

  private String generateRefreshToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private record TokenBundle(String accessToken, String refreshToken) {
  }
}
