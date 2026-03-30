package com.campus.trade.item.repository;

import com.campus.trade.item.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}
