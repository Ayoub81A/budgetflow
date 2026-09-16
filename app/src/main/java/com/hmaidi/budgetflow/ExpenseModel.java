package com.hmaidi.budgetflow;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class ExpenseModel {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String date;
    private double amount;
    private String category;
    private boolean isExpense;
    private int walletId;
    private boolean isRecurring;
    private String recurringInterval; // "daily", "weekly", "monthly", "yearly"
    private long nextDueDate; // timestamp of next due date

    // Constructor for regular expense
    public ExpenseModel(String title, String date,
                        double amount, String category,
                        boolean isExpense, int walletId) {
        this.title = title;
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.isExpense = isExpense;
        this.walletId = walletId;
        this.isRecurring = false;
        this.recurringInterval = "";
        this.nextDueDate = 0;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public boolean isExpense() { return isExpense; }
    public int getWalletId() { return walletId; }
    public boolean isRecurring() { return isRecurring; }
    public String getRecurringInterval() { return recurringInterval; }
    public long getNextDueDate() { return nextDueDate; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setWalletId(int walletId) { this.walletId = walletId; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }
    public void setRecurringInterval(String interval) { recurringInterval = interval; }
    public void setNextDueDate(long nextDueDate) { this.nextDueDate = nextDueDate; }
}