package com.campus.trade.user.repository;

import com.campus.trade.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 用户仓库
 */
public interface UserRepository extends MongoRepository<User, String> {
}
