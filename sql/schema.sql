DROP DATABASE IF EXISTS reimbursement_db;
CREATE DATABASE reimbursement_db;
USE reimbursement_db;

SOURCE sql/production-schema.sql

-- Every seed user has the password pass123
INSERT INTO employees (id, name, email, password_hash, role, manager_id, grade) VALUES
(1, 'Neha Kapoor', 'neha@company.com', SHA2('pass123', 256), 'FINANCE', NULL, 'STAFF'),
(2, 'Rohit Sharma', 'rohit@company.com', SHA2('pass123', 256), 'MANAGER', NULL, 'STAFF'),
(3, 'Priya Mehta', 'priya@company.com', SHA2('pass123', 256), 'MANAGER', NULL, 'STAFF'),
(4, 'Arjun Singh', 'arjun@company.com', SHA2('pass123', 256), 'EMPLOYEE', 2, 'STAFF'),
(5, 'Simran Kaur', 'simran@company.com', SHA2('pass123', 256), 'EMPLOYEE', 2, 'STAFF'),
(6, 'Karan Verma', 'karan@company.com', SHA2('pass123', 256), 'EMPLOYEE', 3, 'STAFF'),
(7, 'Ananya Gupta', 'ananya@company.com', SHA2('pass123', 256), 'EMPLOYEE', 3, 'STAFF'),
(10, 'Neha Kapoor', 'neha@thapar.edu', SHA2('pass123', 256), 'FINANCE', NULL, 'STAFF'),
(11, 'Rohit Sharma', 'rohit@thapar.edu', SHA2('pass123', 256), 'MANAGER', NULL, 'STAFF'),
(12, 'Priya Mehta', 'priya@thapar.edu', SHA2('pass123', 256), 'MANAGER', NULL, 'STAFF'),
(8, 'Riya Sharma', 'riya@thapar.edu', SHA2('pass123', 256), 'EMPLOYEE', 11, 'STUDENT'),
(9, 'Kabir Mehta', 'kabir@thapar.edu', SHA2('pass123', 256), 'EMPLOYEE', 12, 'STUDENT');
