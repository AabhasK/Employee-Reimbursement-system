# Employee Reimbursement and Expense Claim System

A Java 21 web app for Company staff and Thapar students to submit expense claims and for managers and finance to process them.

## Run locally

You need Java 21, Maven, and MySQL 8. With MySQL running, open a terminal in the project root.

1. For a fresh demo, create the database and sample accounts with [sql/schema.sql](sql/schema.sql). This resets any existing `reimbursement_db`.

   ```sh
   mysql -u root -p -e "source sql/schema.sql"
   ```

   On Windows, use the full path to `mysql.exe` if it is not on `PATH`.

2. Copy [db.properties.example](db.properties.example) to `db.properties` and set your MySQL username and password. Git ignores `db.properties`. A `.env` file is unnecessary for the local demo.
3. Run `mvn compile exec:exec` and open [localhost:8080](http://localhost:8080). Press Ctrl+C to stop the server.

## Demo accounts

All demo accounts use `pass123`. Email-code signup is disabled by default.

| Role | Company | Thapar |
| --- | --- | --- |
| Claimant | `arjun@company.com` | `riya@thapar.edu` |
| Manager | `rohit@company.com` | `rohit@thapar.edu` |
| Finance | `neha@company.com` | `neha@thapar.edu` |

Run `mvn test` after setting up the demo database to include the MySQL integration tests. See the [project report](report/report.pdf) for the design and screenshots.
