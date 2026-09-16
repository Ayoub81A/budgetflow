package com.hmaidi.budgetflow;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wallets")
public class Wallet {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String icon;   // stores icon name e.g. "ic_wallet"
    private String color;  // stores hex color e.g. "#7B5EA7"
    private double balance;

    public Wallet(String name, String icon, String color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.balance = 0;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public double getBalance() { return balance; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setBalance(double balance) { this.balance = balance; }
}