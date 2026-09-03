package com.sunrise.clinic.model;

import java.time.LocalDateTime;

public class Dentist {
    private int dentistId;
    private String fullName;
    private DentistStatus status;
    private LocalDateTime createdAt;

    public int getDentistId() {
        return dentistId;
    }

    public void setDentistId(int dentistId) {
        this.dentistId = dentistId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public DentistStatus getStatus() {
        return status;
    }

    public void setStatus(DentistStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return status == DentistStatus.ACTIVE;
    }
}
