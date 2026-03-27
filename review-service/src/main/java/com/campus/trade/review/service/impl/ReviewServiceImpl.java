package com.campus.trade.review.service.impl;

import com.campus.trade.review.dto.CreateReviewRequest;
import com.campus.trade.review.dto.ReviewItemResponse;
import com.campus.trade.review.dto.ReviewStatusResponse;
import com.campus.trade.review.exception.BusinessException;
import com.campus.trade.review.model.Review;
import com.campus.trade.review.model.Trade;
import com.campus.trade.review.model.User;
import com.campus.trade.review.repository.ReviewRepository;
import com.campus.trade.review.repository.TradeRepository;
import com.campus.trade.review.repository.UserRepository;
import com.campus.trade.review.service.ReviewService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             TradeRepository tradeRepository,
                             UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String createReview(String currentUserId, CreateReviewRequest request) {
        Trade trade = tradeRepository.findById(request.getTradeId())
                .orElseThrow(() -> new BusinessException("交易不存在"));

        if (!"COMPLETED".equals(trade.getStatus())) {
            throw new BusinessException("当前交易未完成，不能评价");
        }

        String toUserId;
        if (currentUserId.equals(trade.getBuyerId())) {
            toUserId = trade.getSellerId();
        } else if (currentUserId.equals(trade.getSellerId())) {
            toUserId = trade.getBuyerId();
        } else {
            throw new BusinessException(403, "无权限评价该交易");
        }

        if (reviewRepository.existsByTradeIdAndFromUserId(request.getTradeId(), currentUserId)) {
            throw new BusinessException("您已评价过该交易");
        }

        Review review = new Review();
        review.setTradeId(trade.getId());
        review.setItemId(trade.getItemId());
        review.setFromUserId(currentUserId);
        review.setToUserId(toUserId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(new Date());

        Review savedReview = reviewRepository.save(review);
        return savedReview.getId();
    }

    @Override
    public List<ReviewItemResponse> listUserReviews(String userId) {
        Map<String, String> nicknameCache = new HashMap<>();

        return reviewRepository.findByToUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(review -> toReviewItemResponse(review, nicknameCache))
                .toList();
    }

    @Override
    public ReviewStatusResponse getTradeReviewStatus(String tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException("交易不存在"));

        boolean buyerReviewed = false;
        boolean sellerReviewed = false;

        for (Review review : reviewRepository.findByTradeId(tradeId)) {
            if (trade.getBuyerId().equals(review.getFromUserId())) {
                buyerReviewed = true;
            }
            if (trade.getSellerId().equals(review.getFromUserId())) {
                sellerReviewed = true;
            }
        }

        ReviewStatusResponse response = new ReviewStatusResponse();
        response.setTradeId(tradeId);
        response.setBuyerReviewed(buyerReviewed);
        response.setSellerReviewed(sellerReviewed);
        return response;
    }

    private ReviewItemResponse toReviewItemResponse(Review review, Map<String, String> nicknameCache) {
        ReviewItemResponse response = new ReviewItemResponse();
        response.setReviewId(review.getId());
        response.setTradeId(review.getTradeId());
        response.setFromUserId(review.getFromUserId());
        response.setFromUserNickname(resolveDisplayName(review.getFromUserId(), nicknameCache));
        response.setToUserId(review.getToUserId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    private String resolveDisplayName(String userId, Map<String, String> nicknameCache) {
        if (nicknameCache.containsKey(userId)) {
            return nicknameCache.get(userId);
        }

        String displayName = userRepository.findById(userId)
                .map(this::resolveDisplayName)
                .orElse(userId);

        nicknameCache.put(userId, displayName);
        return displayName;
    }

    private String resolveDisplayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getId();
    }
}
