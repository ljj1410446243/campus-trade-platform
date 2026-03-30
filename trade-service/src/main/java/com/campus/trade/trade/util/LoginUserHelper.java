package com.campus.trade.trade.util;

import com.campus.trade.trade.exception.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class LoginUserHelper {

    private final JwtUtil jwtUtil;

    public LoginUserHelper(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String getCurrentUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new AuthenticationException("未登录或Token缺失");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("Authorization头格式错误");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new AuthenticationException("未登录或Token缺失");
        }
        return jwtUtil.getUserId(token);
    }
}
