package com.campus.trade.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateReviewRequest {

    @NotBlank(message = "tradeId不能为空")
    private String tradeId;

    @NotNull(message = "rating不能为空")
    @Min(value = 1, message = "rating必须在1到5之间")
    @Max(value = 5, message = "rating必须在1到5之间")
    private Integer rating;

    private String comment;

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
