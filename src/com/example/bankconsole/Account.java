package com.example.bankconsole;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DecimalFormat df = new DecimalFormat("#.00");
    

    private final int accountNumber;
    private final String holderName;
    private final int pin; // simple 4-digit pin (int)
    private double balance;
    private final List<Transaction> transactions = new ArrayList<>();

    public Account(int accountNumber, String holderName, int pin, double initialDeposit) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.pin = pin;
        this.balance = initialDeposit;
        transactions.add(new Transaction("OPEN", initialDeposit, balance));
    }

    public int getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    public boolean checkPin(int attempt) { return this.pin == attempt; }
    public double getBalance() { return balance; }
    public List<Transaction> getTransactions() { return new ArrayList<>(transactions); }

    public synchronized void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        balance += amount;
        transactions.add(new Transaction("DEPOSIT", amount, balance));
    }

    public synchronized boolean withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        if (amount > balance) return false;
        balance -= amount;
        transactions.add(new Transaction("WITHDRAW", amount, balance));
        return true;
    }

    // used for transfers: record transfer-out or transfer-in
    public synchronized boolean transferOut(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        if (amount > balance) return false;
        balance -= amount;
        transactions.add(new Transaction("TRANSFER_OUT", amount, balance));
        return true;
    }

    public synchronized void transferIn(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        balance += amount;
        transactions.add(new Transaction("TRANSFER_IN", amount, balance));
    }

    @Override
    public String toString() {
        return String.format("Account{%d} %s - Rs.%s", accountNumber, holderName, df.format(balance));


    }
}
