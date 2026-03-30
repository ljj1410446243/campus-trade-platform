package com.campus.trade.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTradeRequest {

    @NotBlank(message = "itemId不能为空")
    private String itemId;
}
