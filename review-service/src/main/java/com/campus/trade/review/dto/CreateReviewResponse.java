package com.campus.trade.review.dto;

public class CreateReviewResponse {

    private String reviewId;

    public CreateReviewResponse() {
    }

    public CreateReviewResponse(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }
}
