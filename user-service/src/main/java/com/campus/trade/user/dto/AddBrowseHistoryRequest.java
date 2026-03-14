package com.campus.trade.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 新增浏览记录请求 DTO
 */
public class AddBrowseHistoryRequest {

    @NotBlank(message = "itemId不能为空")
    private String itemId;

    /**
     * 可选：SEARCH / RECOMMEND / DIRECT
     */
    private String source;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
