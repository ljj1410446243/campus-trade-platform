package com.campus.trade.auth.repository;

import com.campus.trade.auth.model.RefreshSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshSessionRepository extends MongoRepository<RefreshSession, String> {

  Optional<RefreshSession> findByTokenHash(String tokenHash);
}
