package com.sunrise.clinic.dao;

import com.sunrise.clinic.dao.jdbc.JdbcAppointmentDao;
import com.sunrise.clinic.dao.jdbc.JdbcAuthTokenDao;
import com.sunrise.clinic.dao.jdbc.JdbcBillDao;
import com.sunrise.clinic.dao.jdbc.JdbcDentistDao;
import com.sunrise.clinic.dao.jdbc.JdbcPatientDao;
import com.sunrise.clinic.dao.jdbc.JdbcUserDao;

public enum DaoFactory {
    INSTANCE;

    private final UserDao userDao = new JdbcUserDao();
    private final PatientDao patientDao = new JdbcPatientDao();
    private final AppointmentDao appointmentDao = new JdbcAppointmentDao();
    private final BillDao billDao = new JdbcBillDao();
    private final DentistDao dentistDao = new JdbcDentistDao();
    private final AuthTokenDao authTokenDao = new JdbcAuthTokenDao();

    public UserDao users() {
        return userDao;
    }

    public PatientDao patients() {
        return patientDao;
    }

    public AppointmentDao appointments() {
        return appointmentDao;
    }

    public BillDao bills() {
        return billDao;
    }

    public DentistDao dentists() {
        return dentistDao;
    }

    public AuthTokenDao authTokens() {
        return authTokenDao;
    }
}
