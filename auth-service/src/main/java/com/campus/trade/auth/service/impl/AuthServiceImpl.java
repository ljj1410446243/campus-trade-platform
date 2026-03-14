package com.campus.trade.auth.service.impl;

import com.campus.trade.auth.constant.ErrorCode;
import com.campus.trade.auth.dto.*;
import com.campus.trade.auth.exception.BusinessException;
import com.campus.trade.auth.model.User;
import com.campus.trade.auth.repository.UserRepository;
import com.campus.trade.auth.service.AuthService;
import com.campus.trade.auth.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthService实现
 */
@Service
public class AuthServiceImpl implements AuthService {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  public AuthServiceImpl(JwtUtil jwtUtil,
                         UserRepository userRepository,
                         BCryptPasswordEncoder passwordEncoder) {
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public String ping() {
    return "auth-service is running";
  }

  @Override
  public LoginResponse login(LoginRequest request) {

    User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() ->
                    new BusinessException(ErrorCode.AUTH_ERROR, "用户名或密码错误"));

    boolean passwordMatched = passwordEncoder.matches(
            request.getPassword(),
            user.getPasswordHash()
    );

    if (!passwordMatched) {
      throw new BusinessException(ErrorCode.AUTH_ERROR, "用户名或密码错误");
    }

    String token = jwtUtil.generateToken(user.getId(), user.getUsername());

    return new LoginResponse(token, 7200);
  }

  @Override
  public TokenPayloadResponse parseToken(String token) {
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
}
