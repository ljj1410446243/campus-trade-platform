package com.campus.trade.auth;

import com.campus.trade.auth.model.RefreshSession;
import com.campus.trade.auth.model.User;
import com.campus.trade.auth.repository.RefreshSessionRepository;
import com.campus.trade.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthServiceApplicationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private BCryptPasswordEncoder passwordEncoder;

  @MockBean
  private UserRepository userRepository;

  @MockBean
  private RefreshSessionRepository refreshSessionRepository;

  private final AtomicReference<RefreshSession> storedSession = new AtomicReference<>();

  @BeforeEach
  void setUp() {
    storedSession.set(null);
    when(refreshSessionRepository.save(any(RefreshSession.class))).thenAnswer(invocation -> {
      RefreshSession session = invocation.getArgument(0);
      storedSession.set(session);
      return session;
    });
    when(refreshSessionRepository.findByTokenHash(anyString())).thenAnswer(invocation -> {
      RefreshSession session = storedSession.get();
      String tokenHash = invocation.getArgument(0);
      if (session != null && Objects.equals(session.getTokenHash(), tokenHash)) {
        return Optional.of(session);
      }
      return Optional.empty();
    });
  }

  @Test
  void loginShouldReturnAccessTokenAndRefreshCookie() throws Exception {
    User user = buildUser("password123");
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    MvcResult result = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "JUnit")
                    .content(objectMapper.writeValueAsString(new LoginPayload("alice", "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(900))
            .andReturn();

    String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
    String refreshToken = extractCookieValue(setCookieHeader);

    assertThat(refreshToken).isNotBlank();
    assertThat(storedSession.get()).isNotNull();
    assertThat(storedSession.get().getTokenHash()).isNotEqualTo(refreshToken);
  }

  @Test
  void refreshShouldRotateRefreshTokenAndRejectOldCookie() throws Exception {
    User user = buildUser("password123");
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

    String oldRefreshToken = loginAndExtractRefreshToken();

    MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                    .header("User-Agent", "JUnit")
                    .cookie(new Cookie("refreshToken", oldRefreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();

    String newRefreshToken = extractCookieValue(refreshResult.getResponse().getHeader("Set-Cookie"));
    assertThat(newRefreshToken).isNotBlank();
    assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

    mockMvc.perform(post("/auth/refresh")
                    .header("User-Agent", "JUnit")
                    .cookie(new Cookie("refreshToken", oldRefreshToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("refresh token无效或已过期"))
            .andExpect(result -> assertThat(result.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0"));
  }

  @Test
  void logoutShouldRevokeRefreshSession() throws Exception {
    User user = buildUser("password123");
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

    String refreshToken = loginAndExtractRefreshToken();

    mockMvc.perform(post("/auth/logout")
                    .cookie(new Cookie("refreshToken", refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(result -> assertThat(result.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0"));

    mockMvc.perform(post("/auth/refresh")
                    .header("User-Agent", "JUnit")
                    .cookie(new Cookie("refreshToken", refreshToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  void loginShouldReturn401WhenPasswordIsWrong() throws Exception {
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildUser("password123")));

    mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginPayload("alice", "bad-password"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(4001))
            .andExpect(jsonPath("$.message").value("用户名或密码错误"));
  }

  private String loginAndExtractRefreshToken() throws Exception {
    MvcResult loginResult = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "JUnit")
                    .content(objectMapper.writeValueAsString(new LoginPayload("alice", "password123"))))
            .andExpect(status().isOk())
            .andReturn();
    return extractCookieValue(loginResult.getResponse().getHeader("Set-Cookie"));
  }

  private User buildUser(String rawPassword) {
    User user = new User();
    user.setId("user-1");
    user.setUsername("alice");
    user.setPasswordHash(passwordEncoder.encode(rawPassword));
    user.setRole("USER");
    return user;
  }

  private String extractCookieValue(String setCookieHeader) {
    assertThat(setCookieHeader).isNotBlank();
    String firstPair = setCookieHeader.split(";", 2)[0];
    return firstPair.substring(firstPair.indexOf('=') + 1);
  }

  private record LoginPayload(String username, String password) {
  }
}
