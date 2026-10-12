package com.hmaidi.budgetflow;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "savings_goals")
public class SavingsGoal {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private double targetAmount;
    private double savedAmount;
    private String icon;
    private String color;
    private String targetDate;

    public SavingsGoal(String title, double targetAmount, double savedAmount,
                       String icon, String color, String targetDate) {
        this.title = title;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
        this.icon = icon;
        this.color = color;
        this.targetDate = targetDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getSavedAmount() { return savedAmount; }
    public void setSavedAmount(double savedAmount) { this.savedAmount = savedAmount; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
}
