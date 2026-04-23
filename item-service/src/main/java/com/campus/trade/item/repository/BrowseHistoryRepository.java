package com.campus.trade.item.repository;

import com.campus.trade.item.model.BrowseHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface BrowseHistoryRepository extends MongoRepository<BrowseHistory, String> {

    List<BrowseHistory> findByUserIdOrderByViewedAtDesc(String userId, Pageable pageable);

    List<BrowseHistory> findByViewedAtAfter(Date viewedAt);
}
