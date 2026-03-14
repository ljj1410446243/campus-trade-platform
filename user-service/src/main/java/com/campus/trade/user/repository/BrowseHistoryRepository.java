package com.campus.trade.user.repository;

import com.campus.trade.user.model.BrowseHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 浏览记录仓库
 */
public interface BrowseHistoryRepository extends MongoRepository<BrowseHistory, String> {

    List<BrowseHistory> findByUserIdOrderByViewedAtDesc(String userId);
}
