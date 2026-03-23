package com.campus.trade.chat.repository;

import com.campus.trade.chat.entity.Item;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ItemRepository extends MongoRepository<Item, String> {
}
