package com.campus.trade.item.repository;

import com.campus.trade.item.model.ItemComment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 商品评论仓库
 */
public interface ItemCommentRepository extends MongoRepository<ItemComment, String> {

    List<ItemComment> findByItemId(String itemId);
}
