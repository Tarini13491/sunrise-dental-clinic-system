CREATE DATABASE IF NOT EXISTS sunrise_dental
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE sunrise_dental;

SET FOREIGN_KEY_CHECKS = 0;
DROP VIEW IF EXISTS vw_daily_clinic_summary;
DROP VIEW IF EXISTS vw_dentist_workload;
DROP VIEW IF EXISTS vw_treatment_popularity;
DROP VIEW IF EXISTS vw_revenue_by_month;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS user_sessions;
DROP TABLE IF EXISTS remember_tokens;
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS treatments;
DROP TABLE IF EXISTS dentists;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS clinic_counters;
SET FOREIGN_KEY_CHECKS = 1;

-- --------------------------------------------------------------------------
-- Core tables
-- --------------------------------------------------------------------------

CREATE TABLE users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  CHAR(64)     NOT NULL,
    salt           CHAR(32)     NOT NULL,
    full_name      VARCHAR(120) NOT NULL,
    role           ENUM('ADMIN','RECEPTIONIST','DENTIST') NOT NULL,
    email          VARCHAR(120) NOT NULL,
    phone          VARCHAR(20),
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE patients (
    patient_id     INT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(120) NOT NULL,
    address        VARCHAR(255) NOT NULL,
    contact_number VARCHAR(20)  NOT NULL,
    email          VARCHAR(120),
    date_of_birth  DATE,
    gender         ENUM('MALE','FEMALE','OTHER'),
    notes          VARCHAR(255),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_patient_contact (contact_number),
    INDEX idx_patient_name (full_name)
) ENGINE=InnoDB;

CREATE TABLE dentists (
    dentist_id        INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT,
    full_name         VARCHAR(120) NOT NULL,
    specialization    VARCHAR(100) NOT NULL,
    consultation_fee  DECIMAL(10,2) NOT NULL DEFAULT 2500.00,
    phone             VARCHAR(20),
    email             VARCHAR(120),
    available         TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE treatments (
    treatment_id      INT AUTO_INCREMENT PRIMARY KEY,
    treatment_code    VARCHAR(20)  NOT NULL UNIQUE,
    treatment_name    VARCHAR(120) NOT NULL,
    description       VARCHAR(255),
    category          ENUM('GENERAL','COSMETIC','PEDIATRIC','EMERGENCY','ORTHODONTIC','SURGICAL') NOT NULL DEFAULT 'GENERAL',
    base_cost         DECIMAL(10,2) NOT NULL,
    duration_minutes  INT NOT NULL DEFAULT 30
) ENGINE=InnoDB;

CREATE TABLE clinic_counters (
    counter_date DATE NOT NULL PRIMARY KEY,
    last_seq     INT  NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE appointments (
    appointment_id     INT AUTO_INCREMENT PRIMARY KEY,
    appointment_number VARCHAR(24) NOT NULL UNIQUE,
    patient_id         INT NOT NULL,
    dentist_id         INT NOT NULL,
    treatment_id       INT NOT NULL,
    appointment_date   DATE NOT NULL,
    appointment_time   TIME NOT NULL,
    status             ENUM('SCHEDULED','CHECKED_IN','COMPLETED','CANCELLED','NO_SHOW') NOT NULL DEFAULT 'SCHEDULED',
    notes              VARCHAR(500),
    created_by         INT,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    slot_key           VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE
                WHEN status IN ('CANCELLED', 'NO_SHOW') THEN NULL
                ELSE CONCAT(dentist_id, '-', appointment_date, '-', appointment_time)
            END
        ) STORED,
    FOREIGN KEY (patient_id)   REFERENCES patients(patient_id),
    FOREIGN KEY (dentist_id)   REFERENCES dentists(dentist_id),
    FOREIGN KEY (treatment_id) REFERENCES treatments(treatment_id),
    FOREIGN KEY (created_by)   REFERENCES users(user_id),
    UNIQUE KEY uq_active_dentist_slot (slot_key),
    INDEX idx_appt_date_dentist (appointment_date, dentist_id, appointment_time),
    INDEX idx_appt_number (appointment_number)
) ENGINE=InnoDB;

CREATE TABLE bills (
    bill_id          INT AUTO_INCREMENT PRIMARY KEY,
    bill_number      VARCHAR(24) NOT NULL UNIQUE,
    appointment_id   INT NOT NULL UNIQUE,
    consultation_fee DECIMAL(10,2) NOT NULL,
    treatment_cost   DECIMAL(10,2) NOT NULL,
    surcharge        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax              DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount     DECIMAL(10,2) NOT NULL,
    payment_status   ENUM('UNPAID','PAID','PARTIAL') NOT NULL DEFAULT 'UNPAID',
    payment_method   VARCHAR(30),
    amount_paid      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id)
) ENGINE=InnoDB;

CREATE TABLE notifications (
    notification_id  INT AUTO_INCREMENT PRIMARY KEY,
    appointment_id   INT,
    channel          ENUM('EMAIL','SMS') NOT NULL,
    recipient        VARCHAR(120) NOT NULL,
    subject          VARCHAR(200),
    message          TEXT NOT NULL,
    status           ENUM('PENDING','SENT','FAILED') NOT NULL DEFAULT 'PENDING',
    sent_at          TIMESTAMP NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id)
) ENGINE=InnoDB;

CREATE TABLE audit_log (
    audit_id     INT AUTO_INCREMENT PRIMARY KEY,
    action       VARCHAR(60) NOT NULL,
    table_name   VARCHAR(60),
    record_ref   VARCHAR(60),
    user_id      INT,
    details      TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE user_sessions (
    session_id  VARCHAR(128) PRIMARY KEY,
    user_id     INT NOT NULL,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(255),
    login_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active      TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE remember_tokens (
    token_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    token_hash  CHAR(64) NOT NULL UNIQUE,
    expires_at  DATETIME NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- --------------------------------------------------------------------------
-- Decision-support views (used by management reports)
-- --------------------------------------------------------------------------

CREATE VIEW vw_daily_clinic_summary AS
SELECT
    a.appointment_date,
    COUNT(*) AS total_appointments,
    SUM(a.status = 'COMPLETED') AS completed,
    SUM(a.status = 'CANCELLED') AS cancelled,
    SUM(a.status = 'NO_SHOW') AS no_shows,
    SUM(a.status = 'SCHEDULED') AS scheduled,
    COALESCE(SUM(b.total_amount), 0) AS billed_total,
    COALESCE(SUM(CASE WHEN b.payment_status = 'PAID' THEN b.total_amount ELSE 0 END), 0) AS collected_total
FROM appointments a
LEFT JOIN bills b ON b.appointment_id = a.appointment_id
GROUP BY a.appointment_date;

CREATE VIEW vw_dentist_workload AS
SELECT
    d.dentist_id,
    d.full_name,
    d.specialization,
    COUNT(a.appointment_id) AS appointment_count,
    SUM(a.status = 'COMPLETED') AS completed_count,
    COALESCE(SUM(b.total_amount), 0) AS revenue
FROM dentists d
LEFT JOIN appointments a ON a.dentist_id = d.dentist_id
LEFT JOIN bills b ON b.appointment_id = a.appointment_id
GROUP BY d.dentist_id, d.full_name, d.specialization;

CREATE VIEW vw_treatment_popularity AS
SELECT
    t.treatment_id,
    t.treatment_name,
    t.category,
    COUNT(a.appointment_id) AS times_booked,
    COALESCE(SUM(b.total_amount), 0) AS revenue
FROM treatments t
LEFT JOIN appointments a ON a.treatment_id = t.treatment_id
LEFT JOIN bills b ON b.appointment_id = a.appointment_id
GROUP BY t.treatment_id, t.treatment_name, t.category;

CREATE VIEW vw_revenue_by_month AS
SELECT
    DATE_FORMAT(b.created_at, '%Y-%m') AS month_key,
    COUNT(*) AS bills_issued,
    SUM(b.total_amount) AS total_revenue,
    SUM(CASE WHEN b.payment_status = 'PAID' THEN b.total_amount ELSE 0 END) AS collected,
    SUM(CASE WHEN b.payment_status <> 'PAID' THEN b.total_amount ELSE 0 END) AS outstanding
FROM bills b
GROUP BY DATE_FORMAT(b.created_at, '%Y-%m');
