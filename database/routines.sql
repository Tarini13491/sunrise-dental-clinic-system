USE sunrise_dental;

DROP TRIGGER IF EXISTS trg_prevent_double_booking;
DROP TRIGGER IF EXISTS trg_audit_appointment_insert;
DROP TRIGGER IF EXISTS trg_audit_appointment_update;
DROP TRIGGER IF EXISTS trg_after_bill_insert;
DROP TRIGGER IF EXISTS trg_block_past_appointments;
DROP PROCEDURE IF EXISTS sp_register_appointment;
DROP PROCEDURE IF EXISTS sp_search_appointment;
DROP PROCEDURE IF EXISTS sp_calculate_bill;
DROP PROCEDURE IF EXISTS sp_daily_revenue_report;
DROP PROCEDURE IF EXISTS sp_dentist_performance;
DROP PROCEDURE IF EXISTS sp_mark_bill_paid;
DROP FUNCTION IF EXISTS fn_next_appointment_number;
DROP FUNCTION IF EXISTS fn_next_bill_number;
DROP FUNCTION IF EXISTS fn_calc_tax;
DROP FUNCTION IF EXISTS fn_bill_total;
DROP FUNCTION IF EXISTS fn_treatment_cost;

DELIMITER $$

-- Unique appointment number: SDC-YYYYMMDD-0001
CREATE FUNCTION fn_next_appointment_number(p_date DATE)
RETURNS VARCHAR(24)
NOT DETERMINISTIC
MODIFIES SQL DATA
BEGIN
    DECLARE v_seq INT;
    INSERT INTO clinic_counters (counter_date, last_seq)
    VALUES (p_date, 1)
    ON DUPLICATE KEY UPDATE last_seq = last_seq + 1;
    SELECT last_seq INTO v_seq FROM clinic_counters WHERE counter_date = p_date;
    RETURN CONCAT('SDC-', DATE_FORMAT(p_date, '%Y%m%d'), '-', LPAD(v_seq, 4, '0'));
END$$

CREATE FUNCTION fn_next_bill_number(p_date DATE)
RETURNS VARCHAR(24)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) + 1 INTO v_count FROM bills WHERE DATE(created_at) = p_date;
    RETURN CONCAT('BILL-', DATE_FORMAT(p_date, '%Y%m%d'), '-', LPAD(v_count, 4, '0'));
END$$

-- Sri Lanka VAT assumption: 8% (see docs/ASSUMPTIONS.md)
CREATE FUNCTION fn_calc_tax(p_amount DECIMAL(10,2))
RETURNS DECIMAL(10,2)
DETERMINISTIC
BEGIN
    RETURN ROUND(p_amount * 0.08, 2);
END$$

CREATE FUNCTION fn_bill_total(
    p_consult DECIMAL(10,2),
    p_treatment DECIMAL(10,2),
    p_surcharge DECIMAL(10,2),
    p_discount DECIMAL(10,2)
)
RETURNS DECIMAL(10,2)
DETERMINISTIC
BEGIN
    DECLARE v_sub DECIMAL(10,2);
    SET v_sub = GREATEST(p_consult + p_treatment + IFNULL(p_surcharge,0) - IFNULL(p_discount,0), 0);
    RETURN ROUND(v_sub + fn_calc_tax(v_sub), 2);
END$$

CREATE FUNCTION fn_treatment_cost(p_treatment_id INT)
RETURNS DECIMAL(10,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_cost DECIMAL(10,2);
    SELECT base_cost INTO v_cost FROM treatments WHERE treatment_id = p_treatment_id;
    RETURN IFNULL(v_cost, 0);
END$$

-- --------------------------------------------------------------------------
-- Register a new appointment. Creates or reuses a patient by contact number.
-- --------------------------------------------------------------------------
CREATE PROCEDURE sp_register_appointment(
    IN  p_patient_name     VARCHAR(120),
    IN  p_address          VARCHAR(255),
    IN  p_contact          VARCHAR(20),
    IN  p_email            VARCHAR(120),
    IN  p_dentist_id       INT,
    IN  p_treatment_id     INT,
    IN  p_date             DATE,
    IN  p_time             TIME,
    IN  p_notes            VARCHAR(500),
    IN  p_created_by       INT,
    OUT p_appointment_no   VARCHAR(24),
    OUT p_appointment_id   INT,
    OUT p_message          VARCHAR(255)
)
BEGIN
    DECLARE v_patient_id INT;
    DECLARE v_available TINYINT;
    DECLARE v_clash INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_message = 'Could not save the appointment. Please check the details and try again.';
        SET p_appointment_no = NULL;
        SET p_appointment_id = NULL;
    END;

    START TRANSACTION;

    IF p_patient_name IS NULL OR TRIM(p_patient_name) = '' THEN
        SET p_message = 'Patient name is required.';
        ROLLBACK;
    ELSEIF p_contact IS NULL OR TRIM(p_contact) = '' THEN
        SET p_message = 'Contact number is required.';
        ROLLBACK;
    ELSEIF p_date < CURDATE() THEN
        SET p_message = 'Appointment date cannot be in the past.';
        ROLLBACK;
    ELSEIF TIME(p_time) < TIME('08:00:00') OR TIME(p_time) > TIME('17:30:00') THEN
        SET p_message = 'Clinic hours are 08:00 to 17:30.';
        ROLLBACK;
    ELSE
        SELECT available INTO v_available FROM dentists WHERE dentist_id = p_dentist_id;
        IF v_available IS NULL THEN
            SET p_message = 'Selected dentist was not found.';
            ROLLBACK;
        ELSEIF v_available = 0 THEN
            SET p_message = 'Selected dentist is not available for bookings.';
            ROLLBACK;
        ELSE
            SELECT COUNT(*) INTO v_clash
            FROM appointments
            WHERE dentist_id = p_dentist_id
              AND appointment_date = p_date
              AND appointment_time = p_time
              AND status NOT IN ('CANCELLED','NO_SHOW');

            IF v_clash > 0 THEN
                SET p_message = 'This dentist already has a patient at the selected date and time.';
                ROLLBACK;
            ELSE
                SELECT patient_id INTO v_patient_id
                FROM patients
                WHERE contact_number = p_contact
                ORDER BY patient_id DESC
                LIMIT 1;

                IF v_patient_id IS NULL THEN
                    INSERT INTO patients (full_name, address, contact_number, email)
                    VALUES (p_patient_name, p_address, p_contact, p_email);
                    SET v_patient_id = LAST_INSERT_ID();
                ELSE
                    UPDATE patients
                    SET full_name = p_patient_name,
                        address = p_address,
                        email = COALESCE(p_email, email)
                    WHERE patient_id = v_patient_id;
                END IF;

                SET p_appointment_no = fn_next_appointment_number(p_date);

                INSERT INTO appointments (
                    appointment_number, patient_id, dentist_id, treatment_id,
                    appointment_date, appointment_time, notes, created_by
                ) VALUES (
                    p_appointment_no, v_patient_id, p_dentist_id, p_treatment_id,
                    p_date, p_time, p_notes, p_created_by
                );

                SET p_appointment_id = LAST_INSERT_ID();
                SET p_message = 'Appointment registered successfully.';
                COMMIT;
            END IF;
        END IF;
    END IF;
END$$

-- --------------------------------------------------------------------------
-- Search a complete appointment record by unique appointment number
-- --------------------------------------------------------------------------
CREATE PROCEDURE sp_search_appointment(IN p_appointment_no VARCHAR(24))
BEGIN
    SELECT
        a.appointment_id,
        a.appointment_number,
        a.appointment_date,
        a.appointment_time,
        a.status,
        a.notes,
        a.created_at,
        p.patient_id,
        p.full_name        AS patient_name,
        p.address,
        p.contact_number,
        p.email            AS patient_email,
        d.dentist_id,
        d.full_name        AS dentist_name,
        d.specialization,
        d.consultation_fee,
        t.treatment_id,
        t.treatment_name,
        t.treatment_code,
        t.category         AS treatment_category,
        t.base_cost        AS treatment_cost,
        b.bill_id,
        b.bill_number,
        b.total_amount,
        b.payment_status
    FROM appointments a
    JOIN patients p   ON p.patient_id = a.patient_id
    JOIN dentists d   ON d.dentist_id = a.dentist_id
    JOIN treatments t ON t.treatment_id = a.treatment_id
    LEFT JOIN bills b ON b.appointment_id = a.appointment_id
    WHERE a.appointment_number = p_appointment_no;
END$$

-- --------------------------------------------------------------------------
-- Calculate and persist a bill using DB functions for tax/total
-- --------------------------------------------------------------------------
CREATE PROCEDURE sp_calculate_bill(
    IN  p_appointment_id INT,
    IN  p_discount       DECIMAL(10,2),
    IN  p_surcharge      DECIMAL(10,2),
    OUT p_bill_number    VARCHAR(24),
    OUT p_total          DECIMAL(10,2),
    OUT p_message        VARCHAR(255)
)
BEGIN
    DECLARE v_consult DECIMAL(10,2);
    DECLARE v_treatment DECIMAL(10,2);
    DECLARE v_tax DECIMAL(10,2);
    DECLARE v_total DECIMAL(10,2);
    DECLARE v_existing INT;
    DECLARE v_status VARCHAR(20);
    DECLARE v_category VARCHAR(20);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_message = 'Billing failed. Please try again.';
        SET p_bill_number = NULL;
        SET p_total = NULL;
    END;

    START TRANSACTION;

    SELECT a.status, d.consultation_fee, t.base_cost, t.category
      INTO v_status, v_consult, v_treatment, v_category
    FROM appointments a
    JOIN dentists d ON d.dentist_id = a.dentist_id
    JOIN treatments t ON t.treatment_id = a.treatment_id
    WHERE a.appointment_id = p_appointment_id;

    IF v_status IS NULL THEN
        SET p_message = 'Appointment was not found.';
        ROLLBACK;
    ELSEIF v_status = 'CANCELLED' THEN
        SET p_message = 'Cannot bill a cancelled appointment.';
        ROLLBACK;
    ELSE
        SELECT bill_id INTO v_existing FROM bills WHERE appointment_id = p_appointment_id;
        IF v_existing IS NOT NULL THEN
            SELECT bill_number, total_amount INTO p_bill_number, p_total
            FROM bills WHERE bill_id = v_existing;
            SET p_message = 'A bill already exists for this appointment.';
            ROLLBACK;
        ELSE
            IF v_category = 'EMERGENCY' AND IFNULL(p_surcharge, 0) = 0 THEN
                SET p_surcharge = ROUND(v_treatment * 0.20, 2);
            END IF;
            SET v_total = fn_bill_total(v_consult, v_treatment, IFNULL(p_surcharge,0), IFNULL(p_discount,0));
            SET v_tax = fn_calc_tax(GREATEST(v_consult + v_treatment + IFNULL(p_surcharge,0) - IFNULL(p_discount,0), 0));
            SET p_bill_number = fn_next_bill_number(CURDATE());
            SET p_total = v_total;

            INSERT INTO bills (
                bill_number, appointment_id, consultation_fee, treatment_cost,
                surcharge, discount, tax, total_amount, payment_status
            ) VALUES (
                p_bill_number, p_appointment_id, v_consult, v_treatment,
                IFNULL(p_surcharge,0), IFNULL(p_discount,0), v_tax, v_total, 'UNPAID'
            );

            UPDATE appointments SET status = 'COMPLETED' WHERE appointment_id = p_appointment_id;
            SET p_message = 'Bill calculated successfully.';
            COMMIT;
        END IF;
    END IF;
END$$

CREATE PROCEDURE sp_mark_bill_paid(
    IN  p_bill_number VARCHAR(24),
    IN  p_method      VARCHAR(30),
    IN  p_amount      DECIMAL(10,2),
    OUT p_message     VARCHAR(255)
)
BEGIN
    DECLARE v_total DECIMAL(10,2);
    DECLARE v_bill_id INT;
    SELECT bill_id, total_amount INTO v_bill_id, v_total
    FROM bills WHERE bill_number = p_bill_number;

    IF v_bill_id IS NULL THEN
        SET p_message = 'Bill was not found.';
    ELSE
        UPDATE bills
        SET amount_paid = p_amount,
            payment_method = p_method,
            payment_status = CASE
                WHEN p_amount >= v_total THEN 'PAID'
                WHEN p_amount > 0 THEN 'PARTIAL'
                ELSE 'UNPAID'
            END
        WHERE bill_id = v_bill_id;
        SET p_message = 'Payment recorded.';
    END IF;
END$$

CREATE PROCEDURE sp_daily_revenue_report(IN p_date DATE)
BEGIN
    SELECT * FROM vw_daily_clinic_summary WHERE appointment_date = p_date;
    SELECT
        a.appointment_number,
        p.full_name AS patient_name,
        d.full_name AS dentist_name,
        t.treatment_name,
        a.appointment_time,
        a.status,
        b.bill_number,
        b.total_amount,
        b.payment_status
    FROM appointments a
    JOIN patients p ON p.patient_id = a.patient_id
    JOIN dentists d ON d.dentist_id = a.dentist_id
    JOIN treatments t ON t.treatment_id = a.treatment_id
    LEFT JOIN bills b ON b.appointment_id = a.appointment_id
    WHERE a.appointment_date = p_date
    ORDER BY a.appointment_time;
END$$

CREATE PROCEDURE sp_dentist_performance(IN p_from DATE, IN p_to DATE)
BEGIN
    SELECT
        d.full_name,
        d.specialization,
        COUNT(a.appointment_id) AS appointments,
        SUM(a.status = 'COMPLETED') AS completed,
        SUM(a.status = 'NO_SHOW') AS no_shows,
        COALESCE(SUM(b.total_amount), 0) AS revenue,
        COALESCE(SUM(CASE WHEN b.payment_status = 'PAID' THEN b.total_amount ELSE 0 END), 0) AS collected
    FROM dentists d
    LEFT JOIN appointments a
      ON a.dentist_id = d.dentist_id
     AND a.appointment_date BETWEEN p_from AND p_to
    LEFT JOIN bills b ON b.appointment_id = a.appointment_id
    GROUP BY d.dentist_id, d.full_name, d.specialization
    ORDER BY revenue DESC;
END$$

-- --------------------------------------------------------------------------
-- Triggers — business rules at the database layer
-- --------------------------------------------------------------------------

-- Hours and past-date rules. Double booking is also blocked by uq_active_dentist_slot.
CREATE TRIGGER trg_block_past_appointments
BEFORE INSERT ON appointments
FOR EACH ROW
BEGIN
    IF NEW.appointment_date < CURDATE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot create an appointment in the past.';
    END IF;
    IF NEW.appointment_time < '08:00:00' OR NEW.appointment_time > '17:30:00' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Appointment time is outside clinic hours (08:00–17:30).';
    END IF;
END$$

CREATE TRIGGER trg_audit_appointment_insert
AFTER INSERT ON appointments
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (action, table_name, record_ref, user_id, details)
    VALUES (
        'APPOINTMENT_CREATED',
        'appointments',
        NEW.appointment_number,
        NEW.created_by,
        CONCAT('Patient #', NEW.patient_id, ' with dentist #', NEW.dentist_id,
               ' on ', NEW.appointment_date, ' at ', NEW.appointment_time)
    );
END$$

CREATE TRIGGER trg_audit_appointment_update
AFTER UPDATE ON appointments
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO audit_log (action, table_name, record_ref, details)
        VALUES (
            'APPOINTMENT_STATUS',
            'appointments',
            NEW.appointment_number,
            CONCAT('Status changed from ', OLD.status, ' to ', NEW.status)
        );
    END IF;
END$$

CREATE TRIGGER trg_after_bill_insert
AFTER INSERT ON bills
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (action, table_name, record_ref, details)
    VALUES (
        'BILL_CREATED',
        'bills',
        NEW.bill_number,
        CONCAT('Total LKR ', NEW.total_amount, ' for appointment #', NEW.appointment_id)
    );
END$$

DELIMITER ;
