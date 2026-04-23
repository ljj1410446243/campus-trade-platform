package com.campus.trade.item.repository;

import com.campus.trade.item.model.ReviewSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface ReviewSnapshotRepository extends MongoRepository<ReviewSnapshot, String> {

    List<ReviewSnapshot> findByCreatedAtAfter(Date createdAt);

    List<ReviewSnapshot> findByFromUserIdOrderByCreatedAtDesc(String fromUserId, Pageable pageable);
}
