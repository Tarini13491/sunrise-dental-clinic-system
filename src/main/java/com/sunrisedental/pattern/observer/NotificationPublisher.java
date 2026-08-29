package com.sunrisedental.pattern.observer;

import com.sunrisedental.model.Appointment;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationPublisher {

    private static final Logger LOG = Logger.getLogger(NotificationPublisher.class.getName());
    private static final NotificationPublisher INSTANCE = new NotificationPublisher();

    private final List<AppointmentObserver> observers = new ArrayList<>();

    private NotificationPublisher() {
        observers.add(new EmailNotificationObserver());
        observers.add(new SmsNotificationObserver());
    }

    public static NotificationPublisher get() {
        return INSTANCE;
    }

    public void publish(ClinicEvent event, Appointment appointment, String extra) {
        for (AppointmentObserver observer : observers) {
            try {
                observer.onEvent(event, appointment, extra);
            } catch (RuntimeException ex) {
                LOG.log(Level.WARNING, "Observer failed: " + observer.getClass().getSimpleName(), ex);
            }
        }
    }
}
