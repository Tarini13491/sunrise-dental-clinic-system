package com.sunrisedental.pattern.observer;

import com.sunrisedental.config.AppConfig;
import com.sunrisedental.dao.ReportDao;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.pattern.factory.DaoFactory;

import java.time.format.DateTimeFormatter;

public class EmailNotificationObserver implements AppointmentObserver {

    private final ReportDao reports = DaoFactory.get().reports();

    @Override
    public void onEvent(ClinicEvent event, Appointment appointment, String extra) {
        if (appointment.getPatientEmail() == null || appointment.getPatientEmail().isBlank()) {
            return;
        }
        String clinic = AppConfig.get("clinic.name", "Sunrise Dental Clinic");
        String subject;
        String body;
        switch (event) {
            case APPOINTMENT_BOOKED -> {
                subject = "Your visit is confirmed — " + appointment.getAppointmentNumber();
                body = "Dear " + appointment.getPatientName() + ",\n\n"
                        + "Your appointment at " + clinic + " is confirmed.\n"
                        + "Number: " + appointment.getAppointmentNumber() + "\n"
                        + "Date: " + appointment.getAppointmentDate() + " at " + formatTime(appointment) + "\n"
                        + "Dentist: " + appointment.getDentistName() + "\n"
                        + "Treatment: " + appointment.getTreatmentName() + "\n\n"
                        + "Please arrive 10 minutes early. Reply to this email if you need to reschedule.\n\n"
                        + clinic + "\n" + AppConfig.get("clinic.phone");
            }
            case BILL_ISSUED -> {
                subject = "Receipt " + extra + " from " + clinic;
                body = "Dear " + appointment.getPatientName() + ",\n\n"
                        + "Your treatment bill " + extra + " is ready. You can collect a printed copy at the desk.\n\n"
                        + clinic;
            }
            case APPOINTMENT_CANCELLED -> {
                subject = "Appointment cancelled — " + appointment.getAppointmentNumber();
                body = "Dear " + appointment.getPatientName() + ",\n\nYour appointment "
                        + appointment.getAppointmentNumber() + " has been cancelled. Call us to rebook.\n\n" + clinic;
            }
            default -> {
                subject = clinic + " update";
                body = extra == null ? "Your clinic record was updated." : extra;
            }
        }
        reports.insertNotification(appointment.getAppointmentId(), "EMAIL",
                appointment.getPatientEmail(), subject, body, "SENT");
    }

    private String formatTime(Appointment appointment) {
        if (appointment.getAppointmentTime() == null) {
            return "";
        }
        return appointment.getAppointmentTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
