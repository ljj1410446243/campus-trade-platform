package com.campus.trade.item.util;

import com.campus.trade.item.exception.BusinessException;
import com.campus.trade.item.model.User;
import com.campus.trade.item.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserAccessGuard {

    private static final String STATUS_BANNED = "BANNED";

    private final UserRepository userRepository;

    public UserAccessGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    public User assertWritable(String userId) {
        User user = requireUser(userId);
        if (STATUS_BANNED.equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(403, "当前账号已被封禁，禁止写操作");
        }
        return user;
    }
}
