package com.campus.trade.notice.repository;

import com.campus.trade.notice.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}
