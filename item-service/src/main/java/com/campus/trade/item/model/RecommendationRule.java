package com.campus.trade.item.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "recommendation_rules")
public class RecommendationRule {

    @Id
    private String id;

    @Indexed
    private String scopeType;

    @Indexed
    private String windowType;

    @Indexed
    private String categoryId;

    private List<String> antecedentKeys = new ArrayList<>();
    private String consequentKey;
    private Double support;
    private Double confidence;
    private Double lift;
    private Date updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getWindowType() {
        return windowType;
    }

    public void setWindowType(String windowType) {
        this.windowType = windowType;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public List<String> getAntecedentKeys() {
        return antecedentKeys;
    }

    public void setAntecedentKeys(List<String> antecedentKeys) {
        this.antecedentKeys = antecedentKeys;
    }

    public String getConsequentKey() {
        return consequentKey;
    }

    public void setConsequentKey(String consequentKey) {
        this.consequentKey = consequentKey;
    }

    public Double getSupport() {
        return support;
    }

    public void setSupport(Double support) {
        this.support = support;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Double getLift() {
        return lift;
    }

    public void setLift(Double lift) {
        this.lift = lift;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
