package com.campus.trade.item.repository;

import com.campus.trade.item.model.RecommendationLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecommendationLogRepository extends MongoRepository<RecommendationLog, String> {
}
