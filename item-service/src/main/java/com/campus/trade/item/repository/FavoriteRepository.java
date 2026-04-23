package com.campus.trade.item.repository;

import com.campus.trade.item.model.Favorite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface FavoriteRepository extends MongoRepository<Favorite, String> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Favorite> findByCreatedAtAfter(Date createdAt);
}
