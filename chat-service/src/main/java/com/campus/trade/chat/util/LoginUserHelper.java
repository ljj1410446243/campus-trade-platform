package com.campus.trade.chat.util;

import com.campus.trade.chat.common.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginUserHelper {

    private final JwtUtil jwtUtil;

    public String getCurrentUserId(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new BaseException(401, "未登录或Token缺失");
        }

        try {
            String userId = jwtUtil.getUserIdFromToken(authorization);
            if (userId == null || userId.isBlank()) {
                throw new BaseException(401, "Token无效");
            }
            return userId;
        } catch (Exception e) {
            throw new BaseException(401, "Token无效");
        }
    }
}
