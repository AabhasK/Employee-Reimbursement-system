# Employee Reimbursement and Expense Claim System

A web app for submitting and approving employee expense claims, built for UCS553(enterprise web application) course at Thapar.

Employees submit travel, medical and food claims. Each type checks its own limit and receipt rules. Managers approve claims of their team, claims above Rs 5000 also need finance, and finance marks approved claims as reimbursed. Rejected claims can be fixed and resubmitted.

## Tech used

Java 21, Servlets, Thymeleaf, embedded Apache Tomcat, MySQL, JDBC, JUnit 5, Maven.

## Rules

| Type    | Limit    | Receipt needed |
| ------- | -------- | -------------- |
| Travel  | Rs 25000 | above Rs 1000  |
| Medical | Rs 50000 | always         |
| Food    | Rs 2000  | above Rs 500   |

The expense date cannot be in the future or older than 60 days. A receipt number can only be used once.

## Setup

1. Load the database (it creates `reimbursement_db` with sample users):
   ```
   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "source sql/schema.sql"
   ```
2. Copy `db.properties.example` to `db.properties` and put in your MySQL password.
3. Start the server, then open http://localhost:8080 in a browser:
   ```
   mvn compile exec:exec
   ```
4. Run the tests:
   ```
   mvn test
   ```

## Sample logins

All sample users have the password `pass123`.

| Email             | Role                        |
| ----------------- | --------------------------- |
| neha@company.com  | Finance                     |
| rohit@company.com | Manager of Arjun and Simran |
| priya@company.com | Manager of Karan and Ananya |
| arjun@company.com | Employee                    |

## Project layout

```
src/main/java/com/reimbursement
  model        Claim and its subclasses, Employee, enums
  exception    custom checked exceptions
  dao          DAO interfaces and DBConnection
  dao/jdbc     JDBC implementations
  service      claim, approval, login and report logic
  web          servlets, Thymeleaf setup, messages
  util         AppContext, ReportWriter
src/main/webapp     Thymeleaf templates (WEB-INF/templates) and CSS
sql/schema.sql      tables and sample data
```
