USE reimbursement_db;

ALTER TABLE employees ADD COLUMN university_email VARCHAR(100) NULL UNIQUE;

START TRANSACTION;
UPDATE employees SET email = 'riya@thapar.edu' WHERE email = 'riya@company.com' AND grade = 'STUDENT';
UPDATE employees SET email = 'kabir@thapar.edu' WHERE email = 'kabir@company.com' AND grade = 'STUDENT';
UPDATE employees SET university_email = 'neha@thapar.edu' WHERE email = 'neha@company.com' AND role = 'FINANCE';
UPDATE employees SET university_email = 'rohit@thapar.edu' WHERE email = 'rohit@company.com' AND role = 'MANAGER';
UPDATE employees SET university_email = 'priya@thapar.edu' WHERE email = 'priya@company.com' AND role = 'MANAGER';
COMMIT;
