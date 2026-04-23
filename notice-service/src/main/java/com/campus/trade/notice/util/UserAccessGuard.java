package com.campus.trade.notice.util;

import com.campus.trade.notice.exception.BusinessException;
import com.campus.trade.notice.model.User;
import com.campus.trade.notice.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserAccessGuard {

    private final UserRepository userRepository;

    public UserAccessGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }
}
