DROP DATABASE IF EXISTS reimbursement_db;
CREATE DATABASE reimbursement_db;
USE reimbursement_db;

CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash CHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    manager_id INT NULL,
    FOREIGN KEY (manager_id) REFERENCES employees(id)
);

CREATE TABLE claims (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    expense_date DATE NOT NULL,
    receipt_no VARCHAR(50) NULL UNIQUE,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    reject_reason VARCHAR(255),
    resubmit_count INT NOT NULL DEFAULT 0,
    submitted_at DATETIME NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE claim_audit (
    id INT AUTO_INCREMENT PRIMARY KEY,
    claim_id INT NOT NULL,
    actor_id INT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    note VARCHAR(255),
    at DATETIME NOT NULL,
    FOREIGN KEY (claim_id) REFERENCES claims(id),
    FOREIGN KEY (actor_id) REFERENCES employees(id)
);

-- Every seed user has the password pass123
INSERT INTO employees (id, name, email, password_hash, role, manager_id) VALUES
(1, 'Neha Kapoor', 'neha@company.com', SHA2('pass123', 256), 'FINANCE', NULL),
(2, 'Rohit Sharma', 'rohit@company.com', SHA2('pass123', 256), 'MANAGER', NULL),
(3, 'Priya Mehta', 'priya@company.com', SHA2('pass123', 256), 'MANAGER', NULL),
(4, 'Arjun Singh', 'arjun@company.com', SHA2('pass123', 256), 'EMPLOYEE', 2),
(5, 'Simran Kaur', 'simran@company.com', SHA2('pass123', 256), 'EMPLOYEE', 2),
(6, 'Karan Verma', 'karan@company.com', SHA2('pass123', 256), 'EMPLOYEE', 3),
(7, 'Ananya Gupta', 'ananya@company.com', SHA2('pass123', 256), 'EMPLOYEE', 3);
