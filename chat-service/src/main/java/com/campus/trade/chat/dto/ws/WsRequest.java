package com.campus.trade.chat.dto.ws;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class WsRequest {

    /**
     * 消息类型：AUTH / TEXT
     */
    private String type;

    /**
     * 请求ID，前后端配对用
     */
    private String requestId;

    /**
     * 动态数据体
     */
    private JsonNode data;
}
