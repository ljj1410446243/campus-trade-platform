package com.campus.trade.trade.repository;

import com.campus.trade.trade.model.PickupPoint;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PickupPointRepository extends MongoRepository<PickupPoint, String> {

    List<PickupPoint> findAllByOrderBySortOrderAscCreatedAtDesc();

    List<PickupPoint> findByStatusOrderBySortOrderAscCreatedAtDesc(String status);
}
