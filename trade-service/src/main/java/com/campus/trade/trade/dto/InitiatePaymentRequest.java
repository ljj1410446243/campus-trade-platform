package com.campus.trade.trade.dto;

import com.campus.trade.trade.enums.PayChannel;
import lombok.Data;

@Data
public class InitiatePaymentRequest {

    private PayChannel channel = PayChannel.MOCK;
}
