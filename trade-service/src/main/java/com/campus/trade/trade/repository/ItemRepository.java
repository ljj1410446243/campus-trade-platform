package com.campus.trade.trade.repository;

import com.campus.trade.trade.model.ItemDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ItemRepository extends MongoRepository<ItemDocument, String> {
}
