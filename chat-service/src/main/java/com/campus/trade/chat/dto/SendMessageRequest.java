package com.campus.trade.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "conversationId不能为空")
    private String conversationId;

    @NotBlank(message = "消息类型不能为空")
    private String type;

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
