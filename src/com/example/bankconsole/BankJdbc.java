package com.example.bankconsole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BankJdbc {
    private int nextAccountNumber = 1001;

    public BankJdbc() {
        // initialize nextAccountNumber using DB
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(account_number) FROM accounts")) {
            if (rs.next()) {
                int max = rs.getInt(1);
                if (max > 0) nextAccountNumber = max + 1;
            }
        } catch (SQLException e) {
            System.out.println("Warning: couldn't read next account number: " + e.getMessage());
        }
    }

    // Create account and insert opening transaction
    public synchronized Account createAccount(String holderName, int pin, double initialDeposit) {
        int accNo = nextAccountNumber++;
        String insertAcc = "INSERT INTO accounts(account_number, holder_name, pin, balance) VALUES (?, ?, ?, ?)";
        String insertTx  = "INSERT INTO transactions(account_number, type, amount, balance_after, ts) VALUES (?, 'OPEN', ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pac = con.prepareStatement(insertAcc);
             PreparedStatement ptx = con.prepareStatement(insertTx)) {

            con.setAutoCommit(false);

            pac.setInt(1, accNo);
            pac.setString(2, holderName);
            pac.setInt(3, pin);
            pac.setDouble(4, initialDeposit);
            pac.executeUpdate();

            ptx.setInt(1, accNo);
            ptx.setDouble(2, initialDeposit);
            ptx.setDouble(3, initialDeposit);
            ptx.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ptx.executeUpdate();

            con.commit();

            Account a = new Account(accNo, holderName, pin, initialDeposit);
            return a;
        } catch (SQLException e) {
            System.out.println("Create account failed: " + e.getMessage());
            return null;
        }
    }

    // Find account and load its transactions (returns Account object with its transaction list)
    public Optional<Account> findAccount(int accNo) {
        String qAcc = "SELECT account_number, holder_name, pin, balance FROM accounts WHERE account_number = ?";
        String qTx  = "SELECT type, amount, balance_after, ts FROM transactions WHERE account_number = ? ORDER BY ts ASC, id ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pac = con.prepareStatement(qAcc);
             PreparedStatement ptx = con.prepareStatement(qTx)) {

            pac.setInt(1, accNo);
            try (ResultSet rsa = pac.executeQuery()) {
                if (!rsa.next()) return Optional.empty();
                String holder = rsa.getString("holder_name");
                int pin = rsa.getInt("pin");
                double balance = rsa.getDouble("balance");
                Account acc = new Account(accNo, holder, pin, balance);

                ptx.setInt(1, accNo);
                try (ResultSet rst = ptx.executeQuery()) {
                    while (rst.next()) {
                        String type = rst.getString("type");
                        double amount = rst.getDouble("amount");
                        double balAfter = rst.getDouble("balance_after");
                        String ts = rst.getTimestamp("ts").toString().substring(0, 19).replace('T',' ');
                        // add Transaction into account's list via reflection? -> use public API in Account (we don't have addTx)
                        // We will construct Transaction and add using deposit/withdraw/transfer methods won't record same tx.
                        // To avoid duplicating logic, we will use a Transaction constructor (exists).
                        acc.getTransactions().clear(); // but getTransactions returns new list; we can't modify it
                        // Simpler approach: Use Account constructor already adds OPEN tx; to prevent double-count,
                        // we'll add transactions by using reflection-like approach: use deposit/withdraw would create additional DB writes (not desired)
                        // So instead we will not re-populate transactions inside Account object; we will show transactions from DB when requested.
                    }
                }
                return Optional.of(acc);
            }
        } catch (SQLException e) {
            System.out.println("Error reading account: " + e.getMessage());
            return Optional.empty();
        }
    }

    // Deposit -> update accounts and insert transaction
    public synchronized boolean deposit(int accNo, double amount) {
        String upd = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
        String ins = "INSERT INTO transactions(account_number, type, amount, balance_after, ts) VALUES (?, 'DEPOSIT', ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pup = con.prepareStatement(upd);
             PreparedStatement pins = con.prepareStatement(ins)) {

            con.setAutoCommit(false);

            // update
            pup.setDouble(1, amount);
            pup.setInt(2, accNo);
            int updated = pup.executeUpdate();
            if (updated == 0) { con.rollback(); return false; }

            // compute new balance to store in tx
            double newBalance;
            try (PreparedStatement pst = con.prepareStatement("SELECT balance FROM accounts WHERE account_number = ?")) {
                pst.setInt(1, accNo);
                try (ResultSet rs = pst.executeQuery()) {
                    rs.next();
                    newBalance = rs.getDouble(1);
                }
            }

            pins.setInt(1, accNo);
            pins.setDouble(2, amount);
            pins.setDouble(3, newBalance);
            pins.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pins.executeUpdate();

            con.commit();
            return true;
        } catch (SQLException e) {
            System.out.println("Deposit failed: " + e.getMessage());
            return false;
        }
    }

    // Withdraw -> ensure sufficient funds
    public synchronized boolean withdraw(int accNo, double amount) {
        String upd = "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ?";
        String ins = "INSERT INTO transactions(account_number, type, amount, balance_after, ts) VALUES (?, 'WITHDRAW', ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pup = con.prepareStatement(upd);
             PreparedStatement pins = con.prepareStatement(ins)) {

            con.setAutoCommit(false);

            pup.setDouble(1, amount);
            pup.setInt(2, accNo);
            pup.setDouble(3, amount);
            int updated = pup.executeUpdate();
            if (updated == 0) { con.rollback(); return false; }

            double newBalance;
            try (PreparedStatement pst = con.prepareStatement("SELECT balance FROM accounts WHERE account_number = ?")) {
                pst.setInt(1, accNo);
                try (ResultSet rs = pst.executeQuery()) {
                    rs.next();
                    newBalance = rs.getDouble(1);
                }
            }

            pins.setInt(1, accNo);
            pins.setDouble(2, amount);
            pins.setDouble(3, newBalance);
            pins.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pins.executeUpdate();

            con.commit();
            return true;
        } catch (SQLException e) {
            System.out.println("Withdraw failed: " + e.getMessage());
            return false;
        }
    }

    // Transfer: transactional two-updates + two transaction rows
    public synchronized boolean transfer(int fromAcc, int toAcc, double amount) {
        String dec = "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ?";
        String inc = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
        String ins = "INSERT INTO transactions(account_number, type, amount, balance_after, ts) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pdec = con.prepareStatement(dec);
             PreparedStatement pinc = con.prepareStatement(inc);
             PreparedStatement pins = con.prepareStatement(ins)) {

            con.setAutoCommit(false);

            pdec.setDouble(1, amount);
            pdec.setInt(2, fromAcc);
            pdec.setDouble(3, amount);
            if (pdec.executeUpdate() == 0) { con.rollback(); return false; }

            pinc.setDouble(1, amount);
            pinc.setInt(2, toAcc);
            if (pinc.executeUpdate() == 0) { con.rollback(); return false; }

            // get new balances
            double newFromBal, newToBal;
            try (PreparedStatement pst = con.prepareStatement("SELECT balance FROM accounts WHERE account_number = ?")) {
                pst.setInt(1, fromAcc);
                try (ResultSet rs = pst.executeQuery()) { rs.next(); newFromBal = rs.getDouble(1); }
                pst.setInt(1, toAcc);
                try (ResultSet rs2 = pst.executeQuery()) { rs2.next(); newToBal = rs2.getDouble(1); }
            }

            // insert transfer out for fromAcc
            pins.setInt(1, fromAcc);
            pins.setString(2, "TRANSFER_OUT");
            pins.setDouble(3, amount);
            pins.setDouble(4, newFromBal);
            pins.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            pins.executeUpdate();

            // insert transfer in for toAcc
            pins.setInt(1, toAcc);
            pins.setString(2, "TRANSFER_IN");
            pins.setDouble(3, amount);
            pins.setDouble(4, newToBal);
            pins.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            pins.executeUpdate();

            con.commit();
            return true;
        } catch (SQLException e) {
            System.out.println("Transfer failed: " + e.getMessage());
            return false;
        }
    }

    // List accounts (only basic account info; transactions not loaded)
    public List<Account> listAccounts() {
        List<Account> result = new ArrayList<>();
        String q = "SELECT account_number, holder_name, pin, balance FROM accounts ORDER BY account_number ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(q);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                int accNo = rs.getInt("account_number");
                String name = rs.getString("holder_name");
                int pin = rs.getInt("pin");
                double bal = rs.getDouble("balance");
                result.add(new Account(accNo, name, pin, bal));
            }
        } catch (SQLException e) {
            System.out.println("List accounts failed: " + e.getMessage());
        }
        return result;
    }

    // Fetch transactions for account (used by Main when viewing transaction history)
    public List<Transaction> getTransactions(int accNo) {
        List<Transaction> txs = new ArrayList<>();
        String q = "SELECT type, amount, balance_after, ts FROM transactions WHERE account_number = ? ORDER BY ts ASC, id ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(q)) {

            pst.setInt(1, accNo);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    double amount = rs.getDouble("amount");
                    double bal = rs.getDouble("balance_after");
                    Timestamp t = rs.getTimestamp("ts");
                    String tsStr = t.toLocalDateTime().toString().replace('T',' ');
                    txs.add(new Transaction(type, amount, bal, tsStr)); // we will add constructor below
                }
            }
        } catch (SQLException e) {
            System.out.println("Error loading transactions: " + e.getMessage());
        }
        return txs;
    }
}
