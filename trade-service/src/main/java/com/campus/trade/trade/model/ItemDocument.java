package com.campus.trade.trade.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "items")
public class ItemDocument {

    @Id
    private String id;

    private String sellerId;
    private String title;
    private String description;
    private String categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer conditionStar;
    private List<String> images;
    private String tradeMode;

    /**
     * ON_SALE | RESERVED | SOLD | OFF_SHELF | EXPIRED
     */
    private String status;

    private Date createdAt;
    private Date updatedAt;
}
