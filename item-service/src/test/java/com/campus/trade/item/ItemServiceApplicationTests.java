package com.campus.trade.item;

import com.campus.trade.item.controller.ItemController;
import com.campus.trade.item.dto.ItemListResponse;
import com.campus.trade.item.exception.GlobalExceptionHandler;
import com.campus.trade.item.service.ItemService;
import com.campus.trade.item.util.JwtUtil;
import com.campus.trade.item.util.LoginUserHelper;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@Import({JwtUtil.class, LoginUserHelper.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "jwt.secret=campusTradeSecretKeyCampusTradeSecretKey123456")
class ItemServiceApplicationTests {

  private static final String SECRET = "campusTradeSecretKeyCampusTradeSecretKey123456";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ItemService itemService;

  @Test
  void listMyItemsShouldAcceptValidAccessToken() throws Exception {
    ItemListResponse response = new ItemListResponse();
    response.setItemId("item-1");
    when(itemService.listMyItems("user-1")).thenReturn(List.of(response));

    mockMvc.perform(get("/items/mine")
                    .header("Authorization", "Bearer " + createToken("user-1", "access", SECRET, 900)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].itemId").value("item-1"));
  }

  @Test
  void listMyItemsShouldRejectMissingBearerHeader() throws Exception {
    mockMvc.perform(get("/items/mine"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  void listMyItemsShouldRejectExpiredToken() throws Exception {
    mockMvc.perform(get("/items/mine")
                    .header("Authorization", "Bearer " + createToken("user-1", "access", SECRET, -30)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  void listMyItemsShouldRejectInvalidSignature() throws Exception {
    mockMvc.perform(get("/items/mine")
                    .header("Authorization", "Bearer " + createToken("user-1", "access", "anotherSecretKeyanotherSecretKey123456", 900)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  void listMyItemsShouldRejectWrongTokenType() throws Exception {
    mockMvc.perform(get("/items/mine")
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
