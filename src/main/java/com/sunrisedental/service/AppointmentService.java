package com.sunrisedental.service;

import com.sunrisedental.dao.AppointmentDao;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.User;
import com.sunrisedental.pattern.facade.ClinicFacade;
import com.sunrisedental.pattern.factory.DaoFactory;
import com.sunrisedental.pattern.observer.ClinicEvent;
import com.sunrisedental.pattern.observer.NotificationPublisher;
import com.sunrisedental.util.ValidationUtil;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class AppointmentService {

    private final ClinicFacade facade = new ClinicFacade();
    private final AppointmentDao dao = DaoFactory.get().appointments();

    public Map<String, Object> register(String patientName, String address, String contact, String email,
                                        String dentistId, String treatmentId, String date, String time,
                                        String notes, User staff) {
        String error = ValidationUtil.firstError(
                ValidationUtil.require(patientName, "Patient name"),
                ValidationUtil.require(address, "Address"),
                ValidationUtil.phone(contact),
                ValidationUtil.emailOptional(email),
                ValidationUtil.require(dentistId, "Dentist"),
                ValidationUtil.require(treatmentId, "Treatment type"),
                ValidationUtil.require(date, "Appointment date"),
                ValidationUtil.require(time, "Appointment time")
        );
        if (error != null) {
            return Map.of("success", false, "message", error);
        }
        Date sqlDate;
        Time sqlTime;
        try {
            sqlDate = Date.valueOf(LocalDate.parse(date));
            String normalised = time.length() == 5 ? time + ":00" : time;
            sqlTime = Time.valueOf(LocalTime.parse(normalised));
        } catch (DateTimeParseException ex) {
            return Map.of("success", false, "message", "Use a valid date (YYYY-MM-DD) and time (HH:MM).");
        }
        if (sqlDate.toLocalDate().isBefore(LocalDate.now())) {
            return Map.of("success", false, "message", "Appointment date cannot be in the past.");
        }
        try {
            return facade.registerAppointment(
                    patientName, address, contact, email,
                    Integer.parseInt(dentistId), Integer.parseInt(treatmentId),
                    sqlDate, sqlTime, notes, staff == null ? null : staff.getUserId());
        } catch (NumberFormatException ex) {
            return Map.of("success", false, "message", "Select a dentist and a treatment from the list.");
        }
    }

    public Appointment search(String appointmentNumber) {
        if (appointmentNumber == null || appointmentNumber.isBlank()) {
            return null;
        }
        return dao.findByNumber(appointmentNumber.trim().toUpperCase());
    }

    public List<Appointment> searchFlexible(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String trimmed = query.trim();
        Appointment exact = dao.findByNumber(trimmed.toUpperCase());
        if (exact != null) {
            return List.of(exact);
        }
        return dao.searchFlexible(trimmed);
    }

    public List<Appointment> upcoming() {
        return dao.listUpcoming(12);
    }

    public List<Appointment> byDate(LocalDate date) {
        return dao.listByDate(Date.valueOf(date));
    }

    public Map<String, Object> lookupLists() {
        return Map.of(
                "dentists", dao.listDentists(),
                "treatments", dao.listTreatments()
        );
    }

    public List<String> occupied(int dentistId, LocalDate date) {
        return dao.occupiedSlots(dentistId, Date.valueOf(date));
    }

    public boolean cancel(String appointmentNumber) {
        Appointment existing = search(appointmentNumber);
        if (existing == null) {
            return false;
        }
        boolean ok = dao.updateStatus(appointmentNumber, "CANCELLED");
        if (ok) {
            existing.setStatus("CANCELLED");
            NotificationPublisher.get().publish(ClinicEvent.APPOINTMENT_CANCELLED, existing, null);
        }
        return ok;
    }
}
