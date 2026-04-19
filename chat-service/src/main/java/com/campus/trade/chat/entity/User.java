package com.campus.trade.chat.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
/**
 * users 集合只读视图。
 * chat-service 只能读取会话展示资料，禁止整文档写回用户。
 */
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String username;
    private String nickname;
    private String avatarUrl;
    private Integer creditScore;
    private String creditLevel;
    private Integer reviewCount;
    private Double averageRating;
    private String status;
}
