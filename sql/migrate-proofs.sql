-- Upgrades a database made with the old schema.sql without losing claims.
-- Run once: mysql -u root -p -e "source sql/migrate-proofs.sql"
USE reimbursement_db;

ALTER TABLE employees ADD COLUMN grade VARCHAR(10) NOT NULL DEFAULT 'STAFF';

ALTER TABLE claims ADD COLUMN proof_checked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE claim_proofs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    claim_id INT NOT NULL,
    kind VARCHAR(30) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(100) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    size_bytes INT NOT NULL,
    uploaded_at DATETIME NOT NULL,
    FOREIGN KEY (claim_id) REFERENCES claims(id)
);

INSERT INTO employees (id, name, email, password_hash, role, manager_id, grade) VALUES
(8, 'Riya Sharma', 'riya@company.com', SHA2('pass123', 256), 'EMPLOYEE', 2, 'STUDENT'),
(9, 'Kabir Mehta', 'kabir@company.com', SHA2('pass123', 256), 'EMPLOYEE', 3, 'STUDENT');
