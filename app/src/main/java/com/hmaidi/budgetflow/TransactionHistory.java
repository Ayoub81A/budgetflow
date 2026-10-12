package com.hmaidi.budgetflow;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transaction_history")
public class TransactionHistory {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private int transactionId;
    private long timestamp;
    private String actionType; // "EDIT", "TRASH", "RESTORE"
    private String previousStateJson;
    private String newStateJson;

    public TransactionHistory(int transactionId, long timestamp, String actionType,
                              String previousStateJson, String newStateJson) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.actionType = actionType;
        this.previousStateJson = previousStateJson;
        this.newStateJson = newStateJson;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getPreviousStateJson() { return previousStateJson; }
    public void setPreviousStateJson(String previousStateJson) { this.previousStateJson = previousStateJson; }

    public String getNewStateJson() { return newStateJson; }
    public void setNewStateJson(String newStateJson) { this.newStateJson = newStateJson; }
}
