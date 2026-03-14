package com.campus.trade.user.repository;

import com.campus.trade.user.model.Favorite;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 收藏仓库
 */
public interface FavoriteRepository extends MongoRepository<Favorite, String> {

    Optional<Favorite> findByUserIdAndItemId(String userId, String itemId);

    void deleteByUserIdAndItemId(String userId, String itemId);

    List<Favorite> findByUserIdOrderByCreatedAtDesc(String userId);
}
