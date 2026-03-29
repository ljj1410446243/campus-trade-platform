package com.campus.trade.review.dto;

public class ReviewStatusResponse {

    private String tradeId;
    private boolean buyerReviewed;
    private boolean sellerReviewed;

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public boolean isBuyerReviewed() {
        return buyerReviewed;
    }

    public void setBuyerReviewed(boolean buyerReviewed) {
        this.buyerReviewed = buyerReviewed;
    }

    public boolean isSellerReviewed() {
        return sellerReviewed;
    }

    public void setSellerReviewed(boolean sellerReviewed) {
        this.sellerReviewed = sellerReviewed;
    }
}
