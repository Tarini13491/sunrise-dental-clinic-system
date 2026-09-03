CREATE TABLE IF NOT EXISTS users (
  user_id INT NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL,
  contact_number VARCHAR(20) NOT NULL,
  role VARCHAR(20) NOT NULL,
  account_status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_contact (contact_number),
  KEY idx_users_role_status (role, account_status)
);

CREATE TABLE IF NOT EXISTS patients (
  patient_id INT NOT NULL AUTO_INCREMENT,
  patient_code VARCHAR(20) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  age INT NOT NULL,
  address VARCHAR(255) NOT NULL,
  contact_number VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (patient_id),
  UNIQUE KEY uk_patients_code (patient_code),
  UNIQUE KEY uk_patients_contact (contact_number),
  KEY idx_patients_name (full_name)
);

CREATE TABLE IF NOT EXISTS appointments (
  appointment_id INT NOT NULL AUTO_INCREMENT,
  appointment_number VARCHAR(20) NOT NULL,
  patient_id INT NOT NULL,
  dentist_name VARCHAR(100) NOT NULL,
  treatment_type VARCHAR(80) NOT NULL,
  appointment_date DATE NOT NULL,
  appointment_time TIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_by INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (appointment_id),
  UNIQUE KEY uk_appointments_number (appointment_number),
  KEY idx_appointments_patient (patient_id),
  KEY idx_appointments_slot (dentist_name, appointment_date, appointment_time),
  KEY idx_appointments_status (status),
  CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (patient_id),
  CONSTRAINT fk_appointments_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS bills (
  bill_id INT NOT NULL AUTO_INCREMENT,
  bill_number VARCHAR(20) NOT NULL,
  appointment_id INT NOT NULL,
  treatment_type VARCHAR(80) NOT NULL,
  treatment_cost DECIMAL(10,2) NOT NULL,
  consultation_fee DECIMAL(10,2) NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  issued_by INT NOT NULL,
  issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (bill_id),
  UNIQUE KEY uk_bills_number (bill_number),
  UNIQUE KEY uk_bills_appointment (appointment_id),
  CONSTRAINT fk_bills_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (appointment_id),
  CONSTRAINT fk_bills_issued_by FOREIGN KEY (issued_by) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS dentists (
  dentist_id INT NOT NULL AUTO_INCREMENT,
  full_name VARCHAR(100) NOT NULL,
  dentist_status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (dentist_id),
  UNIQUE KEY uk_dentists_name (full_name),
  KEY idx_dentists_status (dentist_status)
);

CREATE TABLE IF NOT EXISTS auth_tokens (
  token_id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (token_id),
  UNIQUE KEY uk_auth_tokens_hash (token_hash),
  KEY idx_auth_tokens_user (user_id),
  KEY idx_auth_tokens_expires (expires_at),
  CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);
