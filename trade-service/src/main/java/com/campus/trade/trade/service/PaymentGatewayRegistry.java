package com.campus.trade.trade.service;

import com.campus.trade.trade.enums.PayChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayRegistry {

    private final Map<PayChannel, PaymentGateway> gatewayMap = new EnumMap<>(PayChannel.class);

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        for (PaymentGateway gateway : gateways) {
            gatewayMap.put(gateway.getChannel(), gateway);
        }
    }

    public PaymentGateway get(PayChannel channel) {
        return gatewayMap.get(channel);
    }
}
