package com.campus.trade.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 新增收藏请求 DTO
 */
public class AddFavoriteRequest {

    @NotBlank(message = "itemId不能为空")
    private String itemId;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
}
