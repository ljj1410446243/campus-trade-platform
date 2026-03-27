package com.campus.trade.review.service;

import com.campus.trade.review.dto.CreateReviewRequest;
import com.campus.trade.review.dto.ReviewItemResponse;
import com.campus.trade.review.dto.ReviewStatusResponse;

import java.util.List;

public interface ReviewService {

    String createReview(String currentUserId, CreateReviewRequest request);

    List<ReviewItemResponse> listUserReviews(String userId);

    ReviewStatusResponse getTradeReviewStatus(String tradeId);
}
