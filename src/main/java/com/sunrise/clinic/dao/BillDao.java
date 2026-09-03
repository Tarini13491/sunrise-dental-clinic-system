package com.sunrise.clinic.dao;

import com.sunrise.clinic.model.Bill;
import com.sunrise.clinic.model.BillRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillDao {
    Optional<Bill> findById(int billId);

    Optional<BillRecord> findRecordById(int billId);

    Optional<Bill> findByAppointmentId(int appointmentId);

    List<BillRecord> findAll();

    int countForDate(LocalDate date);

    int insert(Bill bill);
}
