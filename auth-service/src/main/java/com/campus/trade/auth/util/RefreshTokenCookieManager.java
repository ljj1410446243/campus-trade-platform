package com.campus.trade.auth.util;

import com.campus.trade.auth.config.RefreshCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieManager {

  private final RefreshCookieProperties refreshCookieProperties;

  public RefreshTokenCookieManager(RefreshCookieProperties refreshCookieProperties) {
    this.refreshCookieProperties = refreshCookieProperties;
  }

  public void writeCookie(HttpServletResponse response, String token) {
    ResponseCookie cookie = ResponseCookie.from(refreshCookieProperties.getName(), token)
            .httpOnly(true)
            .secure(refreshCookieProperties.isSecure())
            .path(refreshCookieProperties.getPath())
            .sameSite(refreshCookieProperties.getSameSite())
            .maxAge(refreshCookieProperties.getMaxAge())
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public void clearCookie(HttpServletResponse response) {
    ResponseCookie cookie = ResponseCookie.from(refreshCookieProperties.getName(), "")
            .httpOnly(true)
            .secure(refreshCookieProperties.isSecure())
            .path(refreshCookieProperties.getPath())
            .sameSite(refreshCookieProperties.getSameSite())
            .maxAge(0)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public String readCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (refreshCookieProperties.getName().equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
