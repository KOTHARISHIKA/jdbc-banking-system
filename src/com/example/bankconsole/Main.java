package com.example.bankconsole;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // Use JDBC-backed bank
        BankJdbc bank = new BankJdbc();
        Scanner sc = new Scanner(System.in);

        try {
            System.out.println("Welcome to Console Bank!");
            while (true) {
                printMainMenu();
                String choice = sc.nextLine().trim();
                if ("1".equals(choice)) {
                    createAccount(bank, sc);
                } else if ("2".equals(choice)) {
                    loginAndOperate(bank, sc);
                } else if ("3".equals(choice)) {
                    listAllAccounts(bank);
                } else if ("0".equals(choice)) {
                    System.out.println("Goodbye!");
                    return;
                } else {
                    System.out.println("Invalid option.");
                }
                System.out.println();
            }
        } finally {
            sc.close();
        }
    }

    private static void printMainMenu() {
        System.out.println("=== Main Menu ===");
        System.out.println("1. Create account");
        System.out.println("2. Login to account");
        System.out.println("3. List all accounts (readonly)");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }

    private static void createAccount(BankJdbc bank, Scanner sc) {
        System.out.print("Holder name: ");
        String name = sc.nextLine().trim();

        int pin;
        while (true) {
            System.out.print("Set a 4-digit PIN (numbers only): ");
            String pinStr = sc.nextLine().trim();
            try {
                pin = Integer.parseInt(pinStr);
                if (pin < 0 || pin > 9999) {
                    throw new NumberFormatException();
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid PIN. Try again.");
            }
        }

        System.out.print("Initial deposit: ");
        double init = readDouble(sc);

        Account acc = bank.createAccount(name, pin, init);
        if (acc != null) {
            System.out.println("Account created: " + acc);
            System.out.println("Remember your account number: " + acc.getAccountNumber());
        } else {
            System.out.println("Failed to create account.");
        }
    }

    private static void loginAndOperate(BankJdbc bank, Scanner sc) {
        System.out.print("Account number: ");
        int accNo = readInt(sc);

        System.out.print("PIN: ");
        int pin = readInt(sc);

        Optional<Account> opt = bank.findAccount(accNo);
        if (!opt.isPresent() || !opt.get().checkPin(pin)) {
            System.out.println("Login failed: invalid account number or PIN.");
            return;
        }

        Account logged = opt.get();
        System.out.println("Welcome, " + logged.getHolderName() + "!");

        while (true) {
            printAccountMenu(logged);
            String ch = sc.nextLine().trim();
            if ("1".equals(ch)) {
                doDeposit(bank, sc, logged.getAccountNumber());
            } else if ("2".equals(ch)) {
                doWithdraw(bank, sc, logged.getAccountNumber());
            } else if ("3".equals(ch)) {
                doTransfer(bank, sc, logged.getAccountNumber());
            } else if ("4".equals(ch)) {
                viewDetails(bank, logged.getAccountNumber());
            } else if ("5".equals(ch)) {
                viewTransactions(bank, logged.getAccountNumber());
            } else if ("0".equals(ch)) {
                System.out.println("Logging out.");
                return;
            } else {
                System.out.println("Invalid option.");
            }
        }
    }

    private static void printAccountMenu(Account acc) {
        System.out.println("\n=== Account Menu (" + acc.getAccountNumber() + ") ===");
        System.out.println("1. Deposit");
        System.out.println("2. Withdraw");
        System.out.println("3. Transfer");
        System.out.println("4. View account details");
        System.out.println("5. View transaction history");
        System.out.println("0. Logout");
        System.out.print("Choose: ");
    }

    private static void doDeposit(BankJdbc bank, Scanner sc, int accNo) {
        System.out.print("Amount to deposit: ");
        double amt = readDouble(sc);
        boolean ok = bank.deposit(accNo, amt);
        System.out.println(ok ? "Deposit successful." : "Deposit failed.");
    }

    private static void doWithdraw(BankJdbc bank, Scanner sc, int accNo) {
        System.out.print("Amount to withdraw: ");
        double amt = readDouble(sc);
        boolean ok = bank.withdraw(accNo, amt);
        System.out.println(ok ? "Withdrawal successful." : "Insufficient funds or failed.");
    }

    private static void doTransfer(BankJdbc bank, Scanner sc, int fromAcc) {
        System.out.print("To account #: ");
        int to = readInt(sc);

        System.out.print("Amount: ");
        double amt = readDouble(sc);

        boolean ok = bank.transfer(fromAcc, to, amt);
        System.out.println(ok ? "Transfer successful." : "Transfer failed. Check balance and account numbers.");
    }

    private static void viewDetails(BankJdbc bank, int accNo) {
        Optional<Account> opt = bank.findAccount(accNo);
        if (opt.isPresent()) {
            System.out.println(opt.get());
        } else {
            System.out.println("Account not found.");
        }
    }

    private static void viewTransactions(BankJdbc bank, int accNo) {
        Optional<Account> opt = bank.findAccount(accNo);
        if (opt.isPresent()) {
            System.out.println("=== Transactions ===");
            List<Transaction> txs = bank.getTransactions(accNo);
            if (txs == null || txs.isEmpty()) {
                System.out.println("No transactions.");
            } else {
                for (Transaction t : txs) {
                    System.out.println(t);
                }
            }
        } else {
            System.out.println("Account not found.");
        }
    }

    private static void listAllAccounts(BankJdbc bank) {
        System.out.println("All accounts:");
        List<Account> all = bank.listAccounts();
        if (all == null || all.isEmpty()) {
            System.out.println("No accounts yet.");
            return;
        }
        for (Account a : all) {
            System.out.println(a);
        }
    }

    // Helper methods
    private static int readInt(Scanner sc) {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.print("Invalid integer. Try again: ");
            }
        }
    }

    private static double readDouble(Scanner sc) {
        while (true) {
            try {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) return 0.0;
                return Double.parseDouble(line);
            } catch (Exception e) {
                System.out.print("Invalid number. Try again: ");
            }
        }
    }
}
