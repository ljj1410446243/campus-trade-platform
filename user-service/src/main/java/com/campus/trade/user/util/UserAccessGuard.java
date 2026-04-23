package com.campus.trade.user.util;

import com.campus.trade.user.exception.BusinessException;
import com.campus.trade.user.model.User;
import com.campus.trade.user.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserAccessGuard {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_BANNED = "BANNED";

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

    public User assertAdmin(String userId) {
        User user = assertWritable(userId);
        if (!ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(403, "无管理员权限");
        }
        return user;
    }
}
