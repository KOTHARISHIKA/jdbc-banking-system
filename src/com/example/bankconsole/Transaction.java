package com.example.bankconsole;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String type; // e.g., "DEPOSIT", "WITHDRAW", "TRANSFER_OUT", "TRANSFER_IN"
    private final double amount;
    private final String timestamp;
    private final double balanceAfter;
    


    public Transaction(String type, double amount, double balanceAfter) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Add this constructor for DB-loaded rows:
    public Transaction(String type, double amount, double balanceAfter, String timestamp) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
    }


    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getTimestamp() { return timestamp; }
    public double getBalanceAfter() { return balanceAfter; }

    @Override
    public String toString() {
        return String.format("%s | %s | Rs.%.2f | Balance: Rs.%.2f", timestamp, type, amount, balanceAfter);


    }
}
