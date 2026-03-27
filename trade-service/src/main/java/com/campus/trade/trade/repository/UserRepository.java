package com.campus.trade.trade.repository;

import com.campus.trade.trade.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}
