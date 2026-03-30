package com.campus.trade.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.trade")
public class TradeProperties {

    private long payTimeoutMinutes = 30;
}
