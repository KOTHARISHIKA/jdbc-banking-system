# jdbc-banking-system
A console-based banking system built using Java, JDBC, and MySQL.

---

## Features

- Create and manage bank accounts
- Deposit & withdraw operations
- Check balance & view transaction history
- JDBC + MySQL integration
- Clean OOP structure (Account, Bank, Transaction, DBConnection)

---

## Tech Stack

- Language: Java
- Database: MySQL
- DB Access: JDBC
- IDE: VS Code / IntelliJ

---

## Project Structure

src/
 └── com/example/bankconsole/
      ├── Main.java
      ├── Account.java
      ├── Bank.java
      ├── Transaction.java
      ├── BankJdbc.java
      └── DBConnection.java

---

## How to Run (local)

Compile:

javac src/com/example/bankconsole/*.java

Run:

java -cp src com.example.bankconsole.Main


---

## Future Enhancements

- Add GUI
- Add login system
- Use PreparedStatements
- Convert to Spring Boot
