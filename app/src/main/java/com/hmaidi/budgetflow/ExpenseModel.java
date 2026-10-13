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
    private boolean isDeleted; // true if in Trash Bin
    private long deletedTimestamp; // timestamp when placed in trash
    private String receiptPath; // local file path to attached receipt photo
    private String notes; // optional note/memo
    private String tags; // optional comma-separated tags, e.g. "groceries, work"

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
        this.isDeleted = false;
        this.deletedTimestamp = 0;
        this.receiptPath = null;
        this.notes = null;
        this.tags = null;
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
    public boolean isDeleted() { return isDeleted; }
    public long getDeletedTimestamp() { return deletedTimestamp; }
    public String getReceiptPath() { return receiptPath; }
    public String getNotes() { return notes; }
    public String getTags() { return tags; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDate(String date) { this.date = date; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCategory(String category) { this.category = category; }
    public void setExpense(boolean expense) { isExpense = expense; }
    public void setWalletId(int walletId) { this.walletId = walletId; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }
    public void setRecurringInterval(String interval) { recurringInterval = interval; }
    public void setNextDueDate(long nextDueDate) { this.nextDueDate = nextDueDate; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public void setDeletedTimestamp(long deletedTimestamp) { this.deletedTimestamp = deletedTimestamp; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setTags(String tags) { this.tags = tags; }
}