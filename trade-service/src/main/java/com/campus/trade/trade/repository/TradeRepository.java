package com.campus.trade.trade.repository;

import com.campus.trade.trade.model.Trade;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TradeRepository extends MongoRepository<Trade, String> {

    List<Trade> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    List<Trade> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    Optional<Trade> findByTradeNo(String tradeNo);

    Optional<Trade> findByOutTradeNo(String outTradeNo);
}
