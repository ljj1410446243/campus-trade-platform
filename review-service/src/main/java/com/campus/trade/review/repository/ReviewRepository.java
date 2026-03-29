package com.campus.trade.review.repository;

import com.campus.trade.review.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByToUserIdOrderByCreatedAtDesc(String toUserId);

    boolean existsByTradeIdAndFromUserId(String tradeId, String fromUserId);

    List<Review> findByTradeId(String tradeId);
}
