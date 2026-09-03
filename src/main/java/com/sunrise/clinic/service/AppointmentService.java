package com.sunrise.clinic.service;

import com.sunrise.clinic.catalog.TreatmentCatalog;
import com.sunrise.clinic.dao.AppointmentDao;
import com.sunrise.clinic.dao.DentistDao;
import com.sunrise.clinic.dao.PatientDao;
import com.sunrise.clinic.exception.ConflictException;
import com.sunrise.clinic.exception.NotFoundException;
import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.Appointment;
import com.sunrise.clinic.model.AppointmentRecord;
import com.sunrise.clinic.model.AppointmentStatus;
import com.sunrise.clinic.model.Patient;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.model.Treatment;
import com.sunrise.clinic.security.AccessPolicy;
import com.sunrise.clinic.validation.FieldValidator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AppointmentService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private final AppointmentDao appointments;
    private final PatientDao patients;
    private final DentistService dentistService;

    public AppointmentService(AppointmentDao appointments, PatientDao patients, DentistDao dentists) {
        this.appointments = appointments;
        this.patients = patients;
        this.dentistService = new DentistService(dentists);
    }

    public List<AppointmentRecord> list(SessionUser actor) {
        AccessPolicy.requireUser(actor);
        return appointments.findAll();
    }

    public AppointmentRecord searchByNumber(SessionUser actor, String appointmentNumber) {
        List<AppointmentRecord> found = search(actor, appointmentNumber);
        return found.get(0);
    }

    public List<AppointmentRecord> search(SessionUser actor, String query) {
        AccessPolicy.requireUser(actor);
        if (query == null || query.isBlank()) {
            throw new ValidationException("Enter a patient name, dentist name, or appointment number.");
        }
        String term = query.trim().replace("%", "").replace("_", "");
        if (term.isBlank()) {
            throw new ValidationException("Enter a patient name, dentist name, or appointment number.");
        }
        if (term.toUpperCase().startsWith("APT")) {
            String number = FieldValidator.appointmentNumber(term);
            return List.of(appointments.findByNumber(number)
                    .orElseThrow(() -> new NotFoundException("No appointment was found for that number.")));
        }
        if (term.length() < 2) {
            throw new ValidationException("Enter at least two letters of the name.");
        }
        List<AppointmentRecord> found = appointments.searchByName(term);
        if (found.isEmpty()) {
            throw new NotFoundException("No appointments were found for that name.");
        }
        return found;
    }

    public AppointmentRecord register(SessionUser actor, Integer patientId, String dentistName, String treatmentType, String date, String time) {
        AccessPolicy.requireStaff(actor);
        if (patientId == null) {
            throw new ValidationException("A patient must be selected.");
        }
        Patient patient = patients.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient record was not found."));
        String dentist = dentistService.requireActive(dentistName).getFullName();
        Treatment treatment = TreatmentCatalog.require(treatmentType);
        LocalDate visitDate = FieldValidator.appointmentDate(date);
        LocalTime visitTime = FieldValidator.appointmentTime(time);
        guardSlot(dentist, patient.getPatientId(), visitDate, visitTime, null);
        Appointment appointment = new Appointment();
        appointment.setAppointmentNumber(nextAppointmentNumber(visitDate));
        appointment.setPatientId(patient.getPatientId());
        appointment.setDentistName(dentist);
        appointment.setTreatmentType(treatment.getName());
        appointment.setAppointmentDate(visitDate);
        appointment.setAppointmentTime(visitTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setCreatedBy(actor.getUserId());
        appointment.setAppointmentId(appointments.insert(appointment));
        return new AppointmentRecord(appointment, patient);
    }

    public AppointmentRecord update(SessionUser actor, int appointmentId, String dentistName, String treatmentType, String date, String time, String statusValue) {
        AccessPolicy.requireStaff(actor);
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment record was not found."));
        Patient patient = patients.findById(appointment.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient record was not found."));
        String dentist = dentistService.requireActive(dentistName).getFullName();
        Treatment treatment = TreatmentCatalog.require(treatmentType);
        LocalDate visitDate = FieldValidator.appointmentDate(date);
        LocalTime visitTime = FieldValidator.appointmentTime(time);
        AppointmentStatus status = parseStatus(statusValue);
        guardSlot(dentist, patient.getPatientId(), visitDate, visitTime, appointment.getAppointmentId());
        appointment.setDentistName(dentist);
        appointment.setTreatmentType(treatment.getName());
        appointment.setAppointmentDate(visitDate);
        appointment.setAppointmentTime(visitTime);
        appointment.setStatus(status);
        appointments.update(appointment);
        return new AppointmentRecord(appointment, patient);
    }

    public AppointmentRecord cancel(SessionUser actor, int appointmentId) {
        AccessPolicy.requireStaff(actor);
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment record was not found."));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ValidationException("This appointment has already been cancelled.");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ValidationException("A completed appointment cannot be cancelled.");
        }
        Patient patient = patients.findById(appointment.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient record was not found."));
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointments.update(appointment);
        return new AppointmentRecord(appointment, patient);
    }

    public AppointmentRecord restore(SessionUser actor, int appointmentId) {
        AccessPolicy.requireStaff(actor);
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment record was not found."));
        if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
            throw new ValidationException("Only a cancelled appointment can be restored.");
        }
        Patient patient = patients.findById(appointment.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient record was not found."));
        dentistService.requireActive(appointment.getDentistName());
        guardSlot(
                appointment.getDentistName(),
                patient.getPatientId(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getAppointmentId()
        );
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointments.update(appointment);
        return new AppointmentRecord(appointment, patient);
    }

    private void guardSlot(String dentist, int patientId, LocalDate date, LocalTime time, Integer excludeId) {
        if (appointments.dentistSlotTaken(dentist, date, time, excludeId)) {
            throw new ConflictException("That dentist already has an appointment at the selected date and time.");
        }
        if (appointments.patientSlotTaken(patientId, date, time, excludeId)) {
            throw new ConflictException("This patient already has an appointment at the selected date and time.");
        }
    }

    private String nextAppointmentNumber(LocalDate date) {
        int sequence = appointments.countForDate(date) + 1;
        return "APT-" + date.format(DAY) + "-" + String.format("%04d", sequence);
    }

    private AppointmentStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return AppointmentStatus.SCHEDULED;
        }
        try {
            return AppointmentStatus.from(value);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Select a valid appointment status.");
        }
    }
}
