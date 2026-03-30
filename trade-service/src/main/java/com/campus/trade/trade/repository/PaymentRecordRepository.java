package com.campus.trade.trade.repository;

import com.campus.trade.trade.model.PaymentRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaymentRecordRepository extends MongoRepository<PaymentRecord, String> {

    Optional<PaymentRecord> findByTradeId(String tradeId);

    Optional<PaymentRecord> findByOutTradeNo(String outTradeNo);
}
