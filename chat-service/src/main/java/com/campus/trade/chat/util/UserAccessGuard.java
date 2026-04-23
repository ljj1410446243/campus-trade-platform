package com.campus.trade.chat.util;

import com.campus.trade.chat.common.BaseException;
import com.campus.trade.chat.entity.User;
import com.campus.trade.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAccessGuard {

    private static final String STATUS_BANNED = "BANNED";

    private final UserRepository userRepository;

    public void assertWritable(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(404, "用户不存在"));
        if (STATUS_BANNED.equalsIgnoreCase(user.getStatus())) {
            throw new BaseException(403, "当前账号已被封禁，禁止写操作");
        }
    }
}
