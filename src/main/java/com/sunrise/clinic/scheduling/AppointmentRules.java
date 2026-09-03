package com.sunrise.clinic.scheduling;

import com.sunrise.clinic.model.Appointment;
import com.sunrise.clinic.model.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public final class AppointmentRules {
    private AppointmentRules() {
    }

    public static boolean occupiesSlot(Appointment appointment, String dentistName, LocalDate date, LocalTime time) {
        if (appointment == null || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            return false;
        }
        return appointment.getDentistName().equalsIgnoreCase(dentistName)
                && appointment.getAppointmentDate().equals(date)
                && appointment.getAppointmentTime().equals(time);
    }

    public static boolean occupiesPatientSlot(Appointment appointment, int patientId, LocalDate date, LocalTime time) {
        if (appointment == null || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            return false;
        }
        return appointment.getPatientId() == patientId
                && appointment.getAppointmentDate().equals(date)
                && appointment.getAppointmentTime().equals(time);
    }
}
