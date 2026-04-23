package com.campus.trade.file.repository;

import com.campus.trade.file.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}
