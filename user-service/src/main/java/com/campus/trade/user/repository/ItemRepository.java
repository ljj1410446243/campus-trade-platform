package com.campus.trade.user.repository;

import com.campus.trade.user.model.Item;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ItemRepository extends MongoRepository<Item, String> {
}
