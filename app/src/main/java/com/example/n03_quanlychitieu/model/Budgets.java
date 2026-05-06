package com.example.n03_quanlychitieu.model;

public class Budgets {
    private String budget_id;
    private double amount;
    private String start_date;
    private String end_date;
    private String description;
    private String user_id;
    private String category_id;
    private int warning_threshold;

    public Budgets() {
    }

    public Budgets (String budget_id, double amount, String start_date, String end_date, String description, String user_id, String category_id) {
        this.budget_id = budget_id;
        this.amount = amount;
        this.start_date = start_date;
        this.end_date = end_date;
        this.description = description;
        this.user_id = user_id;
        this.category_id = category_id;
        this.warning_threshold = 90; // Default to 90%
    }

    public Budgets (String budget_id, double amount, String start_date, String end_date, String description, String user_id, String category_id, int warning_threshold) {
        this.budget_id = budget_id;
        this.amount = amount;
        this.start_date = start_date;
        this.end_date = end_date;
        this.description = description;
        this.user_id = user_id;
        this.category_id = category_id;
        this.warning_threshold = warning_threshold;
    }

    public Budgets(String id, String name) {
        this.budget_id = id;
        this.description = name;
    }

    public String getBudget_id() {
        return budget_id;
    }

    public void setBudget_id(String budget_id) {
        this.budget_id = budget_id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getCategory_id() {
        return category_id;
    }

    public void setCategory_id(String category_id) {
        this.category_id = category_id;
    }

    public int getWarning_threshold() {
        return warning_threshold;
    }

    public void setWarning_threshold(int warning_threshold) {
        this.warning_threshold = warning_threshold;
    }

    // Thêm phương thức này
    @Override
    public String toString() {
        return description; // để Spinner hiển thị tên ngân sách
    }
}
