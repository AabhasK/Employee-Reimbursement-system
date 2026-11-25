CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    manager_id INT NULL,
    grade VARCHAR(10) NOT NULL DEFAULT 'STAFF',
    FOREIGN KEY (manager_id) REFERENCES employees(id)
);

CREATE TABLE claims (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    expense_date DATE NOT NULL,
    receipt_no VARCHAR(50) NULL,
    university BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (university, receipt_no),
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    reject_reason VARCHAR(255),
    resubmit_count INT NOT NULL DEFAULT 0,
    submitted_at DATETIME NOT NULL,
    proof_checked BOOLEAN NOT NULL DEFAULT FALSE,
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

-- Uploaded documents. The file itself is on disk under stored_name, not in the database
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

CREATE TABLE signup_codes (
    email VARCHAR(100) PRIMARY KEY,
    code_hash CHAR(64) NOT NULL,
    issued_at BIGINT NOT NULL,
    attempts INT NOT NULL DEFAULT 0
);
