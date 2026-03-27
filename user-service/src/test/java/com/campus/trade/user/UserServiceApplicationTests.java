package com.campus.trade.user;

import com.campus.trade.user.controller.UserController;
import com.campus.trade.user.dto.UserMeResponse;
import com.campus.trade.user.exception.GlobalExceptionHandler;
import com.campus.trade.user.service.UserService;
import com.campus.trade.user.util.JwtUtil;
import com.campus.trade.user.util.LoginUserHelper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({JwtUtil.class, LoginUserHelper.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "jwt.secret=campusTradeSecretKeyCampusTradeSecretKey123456")
class UserServiceApplicationTests {

  private static final String SECRET = "campusTradeSecretKeyCampusTradeSecretKey123456";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @Test
  void getMeShouldAcceptValidAccessToken() throws Exception {
    when(userService.getMe("user-1"))
            .thenReturn(new UserMeResponse("user-1", "Alice", null, true, 100, "USER"));

    mockMvc.perform(get("/users/me")
                    .header("Authorization", "Bearer " + createToken("user-1", "access", SECRET, 900)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.userId").value("user-1"));
  }

  @Test
  void getMeShouldRejectMissingBearerHeader() throws Exception {
    mockMvc.perform(get("/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  void getMeShouldRejectExpiredToken() throws Exception {
    mockMvc.perform(get("/users/me")
                    .header("Authorization", "Bearer " + createToken("user-1", "access", SECRET, -30)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  void getMeShouldRejectInvalidSignature() throws Exception {
    mockMvc.perform(get("/users/me")
                    .header("Authorization", "Bearer " + createToken("user-1", "access", "anotherSecretKeyanotherSecretKey123456", 900)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  void getMeShouldRejectWrongTokenType() throws Exception {
    mockMvc.perform(get("/users/me")
                    .header("Authorization", "Bearer " + createToken("user-1", "refresh", SECRET, 900)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
  }

  private String createToken(String userId, String type, String secret, long expiresInSeconds) {
    Instant now = Instant.now();
    return Jwts.builder()
            .claim("userId", userId)
            .claim("type", type)
            .subject(userId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expiresInSeconds)))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .compact();
  }
}
