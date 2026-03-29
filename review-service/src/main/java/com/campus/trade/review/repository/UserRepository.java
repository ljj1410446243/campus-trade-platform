package com.campus.trade.review.repository;

import com.campus.trade.review.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}
