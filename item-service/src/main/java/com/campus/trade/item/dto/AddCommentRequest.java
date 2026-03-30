package com.campus.trade.item.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * 评论请求 DTO
 */
public class AddCommentRequest {

    @JsonAlias("comment")
    @NotBlank(message = "content不能为空")
    private String content;

    private Integer rating;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getComment() {
        return content;
    }

    public void setComment(String comment) {
        this.content = comment;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
