package com.campus.trade.file.util;

import com.campus.trade.file.exception.BusinessException;
import com.campus.trade.file.model.User;
import com.campus.trade.file.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserAccessGuard {

    private static final String STATUS_BANNED = "BANNED";

    private final UserRepository userRepository;

    public UserAccessGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void assertWritable(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (STATUS_BANNED.equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(403, "当前账号已被封禁，禁止写操作");
        }
    }
}
