package com.campus.trade.review.repository;

import com.campus.trade.review.model.User;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * users 集合只读访问，禁止在 review-service 中整文档写回用户。
 */
public interface UserRepository extends Repository<User, String> {

    Optional<User> findById(String id);
}
