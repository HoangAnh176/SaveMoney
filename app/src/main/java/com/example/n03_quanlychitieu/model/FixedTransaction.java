package com.example.n03_quanlychitieu.model;

import java.io.Serializable;

public class FixedTransaction implements Serializable {
    private String id;
    private String userId;
    private String type;
    private double amount;
    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private String description;
    private String frequency;
    private String startDate;
    private String endDate;

    public FixedTransaction(String id, String userId, String type, double amount, String categoryId, String categoryName, String categoryIcon, String categoryColor, String description, String frequency, String startDate, String endDate) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryIcon = categoryIcon;
        this.categoryColor = categoryColor;
        this.description = description;
        this.frequency = frequency;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryIcon() { return categoryIcon; }
    public String getCategoryColor() { return categoryColor; }
    public String getDescription() { return description; }
    public String getFrequency() { return frequency; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
}

