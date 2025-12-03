package com.example.bankconsole;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Bank {
    private final List<Account> accounts = new ArrayList<>();
    private int nextAccountNumber = 1001;
    private final File storageFile;
    

    
    public Bank(String storagePath) {
        this.storageFile = new File(storagePath);

        // Create data folder if needed
        if (!this.storageFile.getParentFile().exists()) {
            this.storageFile.getParentFile().mkdirs();
        }

        load();

        // Ensure correct next account number
        for (Account a : accounts) {
            if (a.getAccountNumber() >= nextAccountNumber) {
                nextAccountNumber = a.getAccountNumber() + 1;
            }
        }
    }

    public synchronized Account createAccount(String holderName, int pin, double initialDeposit) {
        Account a = new Account(nextAccountNumber++, holderName, pin, initialDeposit);
        accounts.add(a);
        save();
        return a;
    }

    public Optional<Account> findAccount(int accNo) {
        for (Account a : accounts) {
            if (a.getAccountNumber() == accNo) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    public synchronized boolean deposit(int accNo, double amount) {
        Optional<Account> opt = findAccount(accNo);
        if (!opt.isPresent()) return false;

        Account a = opt.get();
        a.deposit(amount);
        save();
        return true;
    }

    public synchronized boolean withdraw(int accNo, double amount) {
        Optional<Account> opt = findAccount(accNo);
        if (!opt.isPresent()) return false;

        Account a = opt.get();
        boolean ok = a.withdraw(amount);
        if (ok) save();
        return ok;
    }

    public synchronized boolean transfer(int fromAcc, int toAcc, double amount) {
        Optional<Account> srcOpt = findAccount(fromAcc);
        Optional<Account> dstOpt = findAccount(toAcc);

        if (!srcOpt.isPresent() || !dstOpt.isPresent()) return false;

        Account src = srcOpt.get();
        Account dst = dstOpt.get();

        if (!src.transferOut(amount)) return false;

        dst.transferIn(amount);
        save();
        return true;
    }

    public List<Account> listAccounts() {
        return new ArrayList<>(accounts);
    }

    // ------------------ Persistence ------------------ //

    @SuppressWarnings("unchecked")
    private void load() {
        if (!storageFile.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storageFile))) {
            Object obj = ois.readObject();
            if (obj instanceof ArrayList) {
                accounts.clear();
                accounts.addAll((ArrayList<Account>) obj);
            }
        } catch (Exception e) {
            System.out.println("Warning: could not load existing accounts. Starting fresh.");
        }
    }

    private void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFile))) {
            oos.writeObject(new ArrayList<>(accounts));
        } catch (IOException e) {
            System.out.println("Error saving accounts: " + e.getMessage());
        }
    }
}
