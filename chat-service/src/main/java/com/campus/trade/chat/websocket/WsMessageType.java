package com.campus.trade.chat.websocket;

public class WsMessageType {

    private WsMessageType() {
    }

    public static final String AUTH = "AUTH";
    public static final String TEXT = "TEXT";
    public static final String READ = "READ";
    public static final String ACK = "ACK";
    public static final String ERROR = "ERROR";
}
