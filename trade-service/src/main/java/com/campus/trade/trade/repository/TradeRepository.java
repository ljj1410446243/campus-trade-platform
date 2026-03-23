package com.campus.trade.trade.repository;

import com.campus.trade.trade.model.Trade;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TradeRepository extends MongoRepository<Trade, String> {

    List<Trade> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    List<Trade> findBySellerIdOrderByCreatedAtDesc(String sellerId);
}
