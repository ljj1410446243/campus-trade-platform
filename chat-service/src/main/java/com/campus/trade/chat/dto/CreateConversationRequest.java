package com.campus.trade.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateConversationRequest {

    @NotBlank(message = "itemId不能为空")
    private String itemId;

    @NotBlank(message = "sellerId不能为空")
    private String sellerId;
}
