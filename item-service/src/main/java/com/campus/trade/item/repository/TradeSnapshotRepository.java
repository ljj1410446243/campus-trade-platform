package com.campus.trade.item.repository;

import com.campus.trade.item.model.TradeSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface TradeSnapshotRepository extends MongoRepository<TradeSnapshot, String> {

    List<TradeSnapshot> findByBuyerIdOrderByUpdatedAtDesc(String buyerId, Pageable pageable);

    List<TradeSnapshot> findByUpdatedAtAfter(Date updatedAt);
}
