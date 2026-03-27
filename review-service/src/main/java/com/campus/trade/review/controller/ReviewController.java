package com.campus.trade.review.controller;

import com.campus.trade.review.dto.ApiResponse;
import com.campus.trade.review.dto.CreateReviewRequest;
import com.campus.trade.review.dto.CreateReviewResponse;
import com.campus.trade.review.dto.ReviewItemResponse;
import com.campus.trade.review.dto.ReviewStatusResponse;
import com.campus.trade.review.service.ReviewService;
import com.campus.trade.review.util.LoginUserHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final LoginUserHelper loginUserHelper;

    public ReviewController(ReviewService reviewService, LoginUserHelper loginUserHelper) {
        this.reviewService = reviewService;
        this.loginUserHelper = loginUserHelper;
    }

    @PostMapping
    public ApiResponse<CreateReviewResponse> createReview(
            HttpServletRequest request,
            @Valid @RequestBody CreateReviewRequest createReviewRequest) {

        String currentUserId = loginUserHelper.getCurrentUserId(request);
        String reviewId = reviewService.createReview(currentUserId, createReviewRequest);
        return ApiResponse.success(new CreateReviewResponse(reviewId));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<List<ReviewItemResponse>> listUserReviews(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String userId) {

        validateAuthorizationIfPresent(authorizationHeader);
        return ApiResponse.success(reviewService.listUserReviews(userId));
    }

    @GetMapping("/trades/{tradeId}/status")
    public ApiResponse<ReviewStatusResponse> getTradeReviewStatus(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String tradeId) {

        validateAuthorizationIfPresent(authorizationHeader);
        return ApiResponse.success(reviewService.getTradeReviewStatus(tradeId));
    }

    private void validateAuthorizationIfPresent(String authorizationHeader) {
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            loginUserHelper.getCurrentUserId(authorizationHeader);
        }
    }
}
