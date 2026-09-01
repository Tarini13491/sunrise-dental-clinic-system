package com.sunrisedental.pattern.observer;

import com.sunrisedental.model.Appointment;

public interface AppointmentObserver {
    void onEvent(ClinicEvent event, Appointment appointment, String extra);
}
