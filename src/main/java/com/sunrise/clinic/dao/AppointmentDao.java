package com.sunrise.clinic.dao;

import com.sunrise.clinic.model.Appointment;
import com.sunrise.clinic.model.AppointmentRecord;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentDao {
    Optional<Appointment> findById(int appointmentId);

    Optional<AppointmentRecord> findByNumber(String appointmentNumber);

    List<AppointmentRecord> searchByName(String query);

    List<AppointmentRecord> findAll();

    int countForDate(LocalDate date);

    boolean dentistSlotTaken(String dentistName, LocalDate date, LocalTime time, Integer excludeAppointmentId);

    boolean patientSlotTaken(int patientId, LocalDate date, LocalTime time, Integer excludeAppointmentId);

    int insert(Appointment appointment);

    void update(Appointment appointment);
}
