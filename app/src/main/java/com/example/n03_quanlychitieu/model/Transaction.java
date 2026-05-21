package com.example.n03_quanlychitieu.model;

public class Transaction {
    public String id;
    public double amount;
    public String description;
    public String date; // ISO 8601 string
    public String type; // "income" or "expense"
    public String categoryName;
    public String categoryIcon;
    public String categoryColor;
    public String categoryId;
    public String budgetId;
    public String fixedId;

    public Transaction(String id, double amount, String description, String date, String type,
                       String categoryName, String categoryIcon, String categoryColor, String categoryId, String budgetId, String fixedId) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.type = type;
        this.categoryName = categoryName;
        this.categoryIcon = categoryIcon;
        this.categoryColor = categoryColor;
        this.categoryId = categoryId;
        this.budgetId = budgetId;
        this.fixedId = fixedId;
    }
}
