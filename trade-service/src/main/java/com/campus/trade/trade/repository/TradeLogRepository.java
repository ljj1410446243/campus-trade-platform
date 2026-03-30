package com.campus.trade.trade.repository;

import com.campus.trade.trade.model.TradeLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TradeLogRepository extends MongoRepository<TradeLog, String> {

    List<TradeLog> findByTradeIdOrderByCreatedAtAsc(String tradeId);
}
