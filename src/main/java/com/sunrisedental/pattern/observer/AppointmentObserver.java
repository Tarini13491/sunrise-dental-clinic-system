package com.sunrisedental.pattern.observer;

import com.sunrisedental.model.Appointment;

/**
 * Observer pattern — appointment and billing events fan out to email, SMS
 * and the audit trail without the booking service knowing those details.
 *
 * Why: the clinic wants patients to receive confirmation messages. If the
 * AppointmentService called JavaMail and an SMS gateway directly, adding a
 * third channel (WhatsApp, printed slip) would mean editing core booking
 * code. Observers subscribe instead.
 *
 * Evaluation: Observer reduces coupling and is a natural fit for
 * notifications. The risk is silent failure if an observer throws; this
 * implementation isolates each observer in a try/catch so one channel
 * cannot block the others.
 */
public interface AppointmentObserver {
    void onEvent(ClinicEvent event, Appointment appointment, String extra);
}
