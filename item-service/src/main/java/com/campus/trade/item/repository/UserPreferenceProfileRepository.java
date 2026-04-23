package com.campus.trade.item.repository;

import com.campus.trade.item.model.UserPreferenceProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserPreferenceProfileRepository extends MongoRepository<UserPreferenceProfile, String> {

    Optional<UserPreferenceProfile> findByUserId(String userId);
}
