package com.sunrise.clinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BillRecord {
    private Bill bill;
    private Appointment appointment;
    private Patient patient;

    public BillRecord(Bill bill, Appointment appointment, Patient patient) {
        this.bill = bill;
        this.appointment = appointment;
        this.patient = patient;
    }

    public Bill getBill() {
        return bill;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getBillNumber() {
        return bill.getBillNumber();
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

    public String getTreatmentType() {
        return bill.getTreatmentType();
    }

    public BigDecimal getTreatmentCost() {
        return bill.getTreatmentCost();
    }

    public BigDecimal getConsultationFee() {
        return bill.getConsultationFee();
    }

    public BigDecimal getTotalAmount() {
        return bill.getTotalAmount();
    }

    public LocalDateTime getIssuedAt() {
        return bill.getIssuedAt();
    }
}
