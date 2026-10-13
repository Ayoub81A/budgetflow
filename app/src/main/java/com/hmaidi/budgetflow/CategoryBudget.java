package com.hmaidi.budgetflow;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category_budgets")
public class CategoryBudget {

    @PrimaryKey
    @NonNull
    private String categoryName;
    private double monthlyLimit;

    public CategoryBudget(@NonNull String categoryName, double monthlyLimit) {
        this.categoryName = categoryName;
        this.monthlyLimit = monthlyLimit;
    }

    @NonNull
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(@NonNull String categoryName) {
        this.categoryName = categoryName;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }
}
