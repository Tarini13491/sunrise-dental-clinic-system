package com.sunrise.clinic.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentRecord {
    private Appointment appointment;
    private Patient patient;

    public AppointmentRecord(Appointment appointment, Patient patient) {
        this.appointment = appointment;
        this.patient = patient;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getAppointmentNumber() {
        return appointment.getAppointmentNumber();
    }

    public String getPatientName() {
        return patient.getFullName();
    }

    public int getAge() {
        return patient.getAge();
    }

    public String getAddress() {
        return patient.getAddress();
    }

    public String getContactNumber() {
        return patient.getContactNumber();
    }

    public String getDentistName() {
        return appointment.getDentistName();
    }

    public String getTreatmentType() {
        return appointment.getTreatmentType();
    }

    public LocalDate getAppointmentDate() {
        return appointment.getAppointmentDate();
    }

    public LocalTime getAppointmentTime() {
        return appointment.getAppointmentTime();
    }
}
