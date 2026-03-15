package com.campus.trade.item.repository;

import com.campus.trade.item.model.Item;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ItemRepository extends MongoRepository<Item, String> {

    List<Item> findBySellerIdOrderByCreatedAtDesc(String sellerId);
}
