package com.sunrisedental.pattern.observer;

import com.sunrisedental.config.AppConfig;
import com.sunrisedental.dao.ReportDao;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.pattern.factory.DaoFactory;

/**
 * Simulated SMS gateway (Dialog / Mobitel style) for the Colombo clinic.
 * Messages are persisted so staff can prove a reminder went out even when
 * a real SMS API key is not configured for the student environment.
 */
public class SmsNotificationObserver implements AppointmentObserver {

    private final ReportDao reports = DaoFactory.get().reports();

    @Override
    public void onEvent(ClinicEvent event, Appointment appointment, String extra) {
        if (appointment.getContactNumber() == null || appointment.getContactNumber().isBlank()) {
            return;
        }
        String clinic = AppConfig.get("clinic.name", "Sunrise Dental");
        String text = switch (event) {
            case APPOINTMENT_BOOKED -> clinic + ": Hi " + firstName(appointment) + ", your visit "
                    + appointment.getAppointmentNumber() + " is on " + appointment.getAppointmentDate()
                    + " at " + appointment.getAppointmentTime() + " with " + appointment.getDentistName() + ".";
            case BILL_ISSUED -> clinic + ": Bill " + extra + " is ready at the front desk. Thank you.";
            case APPOINTMENT_CANCELLED -> clinic + ": Appointment " + appointment.getAppointmentNumber()
                    + " was cancelled. Call " + AppConfig.get("clinic.phone") + " to rebook.";
            case PAYMENT_RECEIVED -> clinic + ": Payment received. Thank you for visiting us.";
        };
        reports.insertNotification(appointment.getAppointmentId(), "SMS",
                appointment.getContactNumber(), "SMS reminder", text, "SENT");
    }

    private String firstName(Appointment appointment) {
        String name = appointment.getPatientName();
        if (name == null || name.isBlank()) {
            return "there";
        }
        return name.split(" ")[0];
    }
}
