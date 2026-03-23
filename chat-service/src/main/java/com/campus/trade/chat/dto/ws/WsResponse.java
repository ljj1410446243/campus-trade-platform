package com.campus.trade.chat.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsResponse {

    /**
     * ACK / ERROR / TEXT
     */
    private String type;

    /**
     * 与请求对应
     */
    private String requestId;

    /**
     * 返回数据
     */
    private Object data;

    public static WsResponse ack(String requestId, Object data) {
        return new WsResponse("ACK", requestId, data);
    }

    public static WsResponse error(String requestId, String message) {
        return new WsResponse("ERROR", requestId, new ErrorBody(message));
    }

    public static WsResponse text(Object data) {
        return new WsResponse("TEXT", null, data);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorBody {
        private String message;
    }
}
