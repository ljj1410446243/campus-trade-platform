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
import com.campus.trade.review.util.UserAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private static final int DEFAULT_CREDIT_SCORE = 0;
    private static final int DEFAULT_REVIEW_COUNT = 0;
    private static final double DEFAULT_AVERAGE_RATING = 0D;
    private static final String CREDIT_LEVEL_NEW = "NEW";
    private static final String CREDIT_LEVEL_EXCELLENT = "EXCELLENT";
    private static final String CREDIT_LEVEL_GOOD = "GOOD";
    private static final String CREDIT_LEVEL_FAIR = "FAIR";
    private static final String CREDIT_LEVEL_LOW = "LOW";

    private final ReviewRepository reviewRepository;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final UserAccessGuard userAccessGuard;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             TradeRepository tradeRepository,
                             UserRepository userRepository,
                             MongoTemplate mongoTemplate,
                             UserAccessGuard userAccessGuard) {
        this.reviewRepository = reviewRepository;
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.userAccessGuard = userAccessGuard;
    }

    @Override
    public String createReview(String currentUserId, CreateReviewRequest request) {
        userAccessGuard.assertWritable(currentUserId);
        String tradeId = request.getTradeId();
        Trade trade = tradeRepository.findById(request.getTradeId())
                .orElseThrow(() -> new BusinessException(404, "交易不存在"));

        validateTradeParticipants(trade);

        if (!"COMPLETED".equals(trade.getStatus())) {
            throw new BusinessException(409, "当前交易未完成，不能评价");
        }

        String toUserId = resolveToUserId(currentUserId, trade);

        boolean exists = reviewRepository.existsByTradeIdAndFromUserId(request.getTradeId(), currentUserId);
        log.info("create-review check tradeId={} currentUserId={} status={} buyerId={} sellerId={} toUserId={} existsByTradeIdAndFromUserId={}",
                tradeId,
                currentUserId,
                trade.getStatus(),
                trade.getBuyerId(),
                trade.getSellerId(),
                toUserId,
                exists);
        if (exists) {
            throw new BusinessException(409, "您已评价过该交易");
        }

        log.info("create-review load-target-user tradeId={} currentUserId={} toUserId={}", tradeId, currentUserId, toUserId);
        userRepository.findById(toUserId)
                .orElseThrow(() -> new BusinessException(409, "被评价用户不存在"));

        Review review = new Review();
        review.setTradeId(trade.getId());
        review.setItemId(trade.getItemId());
        review.setFromUserId(currentUserId);
        review.setToUserId(toUserId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(new Date());

        log.info("create-review save-start tradeId={} currentUserId={} toUserId={}", tradeId, currentUserId, toUserId);
        Review savedReview = reviewRepository.save(review);
        log.info("create-review save-done tradeId={} currentUserId={} toUserId={} reviewId={}",
                tradeId, currentUserId, toUserId, savedReview.getId());
        log.info("create-review credit-recalc-start tradeId={} currentUserId={} toUserId={}", tradeId, currentUserId, toUserId);
        try {
            recalculateCreditProfile(toUserId);
            log.info("create-review credit-recalc-done tradeId={} currentUserId={} toUserId={} reviewId={}",
                    tradeId, currentUserId, toUserId, savedReview.getId());
        } catch (RuntimeException e) {
            log.error("create-review credit-recalc-failed tradeId={} currentUserId={} toUserId={} reviewId={} exceptionType={}",
                    tradeId,
                    currentUserId,
                    toUserId,
                    savedReview.getId(),
                    e.getClass().getName(),
                    e);
        }
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
                .orElseThrow(() -> new BusinessException(404, "交易不存在"));
        validateTradeParticipants(trade);

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

    private void recalculateCreditProfile(String userId) {
        if (isBlank(userId)) {
            throw new BusinessException(409, "评价目标非法，暂时不能更新信用分");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(409, "被评价用户不存在"));

        List<Review> reviews = reviewRepository.findByToUserId(userId);
        int reviewCount = reviews.size();
        if (reviewCount == 0) {
            updateCreditProfile(
                    user.getId(),
                    DEFAULT_CREDIT_SCORE,
                    CREDIT_LEVEL_NEW,
                    DEFAULT_REVIEW_COUNT,
                    DEFAULT_AVERAGE_RATING
            );
            return;
        }

        double ratingSum = 0D;
        for (Review review : reviews) {
            ratingSum += review.getRating() == null ? 0D : review.getRating();
        }

        double averageRating = ratingSum / reviewCount;
        int creditScore = Math.min(100, (int) Math.round(averageRating * 18 + Math.min(reviewCount, 10)));

        updateCreditProfile(
                user.getId(),
                creditScore,
                resolveCreditLevel(creditScore, reviewCount),
                reviewCount,
                averageRating
        );
    }

    private void updateCreditProfile(String userId,
                                     int creditScore,
                                     String creditLevel,
                                     int reviewCount,
                                     double averageRating) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(userId)),
                new Update()
                        .set("creditScore", creditScore)
                        .set("creditLevel", creditLevel)
                        .set("reviewCount", reviewCount)
                        .set("averageRating", averageRating),
                User.class
        );
    }

    private String resolveCreditLevel(int creditScore, int reviewCount) {
        if (reviewCount == 0) {
            return CREDIT_LEVEL_NEW;
        }
        if (creditScore >= 90) {
            return CREDIT_LEVEL_EXCELLENT;
        }
        if (creditScore >= 75) {
            return CREDIT_LEVEL_GOOD;
        }
        if (creditScore >= 60) {
            return CREDIT_LEVEL_FAIR;
        }
        return CREDIT_LEVEL_LOW;
    }

    private void validateTradeParticipants(Trade trade) {
        if (isBlank(trade.getBuyerId()) || isBlank(trade.getSellerId())) {
            throw new BusinessException(409, "交易缺少买卖双方信息，暂时不能评价");
        }
    }

    private String resolveToUserId(String currentUserId, Trade trade) {
        String toUserId;
        if (currentUserId.equals(trade.getBuyerId())) {
            toUserId = trade.getSellerId();
        } else if (currentUserId.equals(trade.getSellerId())) {
            toUserId = trade.getBuyerId();
        } else {
            throw new BusinessException(403, "无权限评价该交易");
        }

        if (isBlank(toUserId) || currentUserId.equals(toUserId)) {
            throw new BusinessException(409, "评价目标非法，暂时不能评价");
        }
        return toUserId;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
