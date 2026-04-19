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
import java.util.regex.Pattern;

/**
 * AuthService实现
 */
@Service
public class AuthServiceImpl implements AuthService {

  private static final Pattern MAINLAND_CHINA_PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

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
    String account = normalize(request.getAccount());
    String password = normalize(request.getPassword());

    if (account == null || account.isBlank() || password == null || password.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "账号和密码不能为空");
    }

    User user = userRepository.findByUsername(account)
            .or(() -> userRepository.findByPhone(account))
            .orElseThrow(() ->
                    new BusinessException(ErrorCode.AUTH_ERROR, HttpStatus.UNAUTHORIZED, "账号或密码错误"));

    String passwordHash = user.getPasswordHash();
    if (passwordHash == null || passwordHash.isBlank()) {
      throw new BusinessException(ErrorCode.AUTH_ERROR, HttpStatus.UNAUTHORIZED, "账号或密码错误");
    }

    boolean passwordMatched;
    try {
      passwordMatched = passwordEncoder.matches(password, passwordHash);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.AUTH_ERROR, HttpStatus.UNAUTHORIZED, "账号或密码错误");
    }

    if (!passwordMatched) {
      throw new BusinessException(ErrorCode.AUTH_ERROR, HttpStatus.UNAUTHORIZED, "账号或密码错误");
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
    String username = normalize(request.getUsername());
    String phone = normalize(request.getPhone());
    String nickname = normalize(request.getNickname());
    String password = normalize(request.getPassword());

    if (username == null || username.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "用户名不能为空");
    }

    if (phone == null || phone.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "手机号不能为空");
    }

    if (nickname == null || nickname.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "昵称不能为空");
    }

    if (password == null || password.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "密码不能为空");
    }

    boolean usernameExists = userRepository.findByUsername(username).isPresent();

    if (usernameExists) {
      throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
    }

    if (!MAINLAND_CHINA_PHONE_PATTERN.matcher(phone).matches()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号格式不正确");
    }

    if (userRepository.existsByPhone(phone)) {
      throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "手机号已存在");
    }

    User user = new User();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setPhone(phone);
    user.setNickname(nickname);
    user.setRole("USER");
    user.setStatus("ACTIVE");
    user.setCampusVerified(false);

    userRepository.save(user);
  }

  @Override
  public void campusVerify(String userId, CampusVerifyRequest request) {

    User user = userRepository.findById(userId)
            .orElseThrow(() ->
                    new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

    user.setRealName(normalize(request.getRealName()));
    user.setCampusVerified(true);
    user.setStudentId(normalize(request.getStudentId()));

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

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }

  private record TokenBundle(String accessToken, String refreshToken) {
  }
}
