USE reimbursement_db;

ALTER TABLE employees MODIFY COLUMN password_hash VARCHAR(255) NOT NULL;
ALTER TABLE claims ADD COLUMN university BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE signup_codes (
    email VARCHAR(100) PRIMARY KEY,
    code_hash CHAR(64) NOT NULL,
    issued_at BIGINT NOT NULL,
    attempts INT NOT NULL DEFAULT 0
);

START TRANSACTION;
INSERT INTO employees (name, email, password_hash, role, grade)
SELECT name, university_email, password_hash, role, grade FROM employees
WHERE university_email IN ('neha@thapar.edu', 'rohit@thapar.edu', 'priya@thapar.edu');

UPDATE employees student
JOIN employees old_manager ON student.manager_id = old_manager.id
JOIN employees new_manager ON new_manager.email = old_manager.university_email
SET student.manager_id = new_manager.id
WHERE student.grade = 'STUDENT' AND student.email LIKE '%@thapar.edu';

UPDATE claims c JOIN employees e ON e.id = c.employee_id
SET c.university = TRUE WHERE e.email LIKE '%@thapar.edu';
COMMIT;

ALTER TABLE claims DROP INDEX receipt_no, ADD UNIQUE (university, receipt_no);
