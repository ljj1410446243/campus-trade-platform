package com.campus.trade.item.repository;

import com.campus.trade.item.model.RecommendationRule;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecommendationRuleRepository extends MongoRepository<RecommendationRule, String> {
}
