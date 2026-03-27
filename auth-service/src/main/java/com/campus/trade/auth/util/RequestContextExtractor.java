package com.campus.trade.auth.util;

import com.campus.trade.auth.service.ClientContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestContextExtractor {

  public ClientContext extract(HttpServletRequest request) {
    return new ClientContext(resolveUserAgent(request), resolveClientIp(request));
  }

  private String resolveUserAgent(HttpServletRequest request) {
    return request.getHeader("User-Agent");
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
