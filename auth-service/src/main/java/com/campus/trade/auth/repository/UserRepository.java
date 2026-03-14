package com.campus.trade.auth.repository;

import com.campus.trade.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 用户仓库
 */
public interface UserRepository extends MongoRepository<User, String> {

  Optional<User> findByUsername(String username);

}
