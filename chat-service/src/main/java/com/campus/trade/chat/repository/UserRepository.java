package com.campus.trade.chat.repository;

import com.campus.trade.chat.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}
