package com.campus.trade.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新用户资料请求 DTO
 */
public class UpdateUserProfileRequest {

    @NotBlank(message = "nickname不能为空")
    private String nickname;

    private String avatarUrl;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
