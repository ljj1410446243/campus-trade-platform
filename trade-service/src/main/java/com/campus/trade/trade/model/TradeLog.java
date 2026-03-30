package com.campus.trade.trade.model;

import com.campus.trade.trade.enums.TradeLogAction;
import com.campus.trade.trade.enums.TradeStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "trade_logs")
public class TradeLog {

    @Id
    private String id;

    private String tradeId;
    private String tradeNo;
    private TradeLogAction action;
    private TradeStatus fromStatus;
    private TradeStatus toStatus;
    private String operatorId;
    private String operatorRole;
    private String message;
    private Date createdAt;
}
