package com.sunrise.clinic.service;

import com.sunrise.clinic.dao.DaoFactory;

public enum ClinicServices {
    INSTANCE;

    private final AuthService authService;
    private final StaffService staffService;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final BillingService billingService;
    private final ReportService reportService;
    private final DentistService dentistService;

    ClinicServices() {
        DaoFactory factory = DaoFactory.INSTANCE;
        this.authService = new AuthService(factory.users(), factory.authTokens());
        this.staffService = new StaffService(factory.users(), factory.authTokens());
        this.patientService = new PatientService(factory.patients());
        this.appointmentService = new AppointmentService(factory.appointments(), factory.patients(), factory.dentists());
        this.billingService = new BillingService(factory.bills(), factory.appointments(), factory.patients());
        this.reportService = new ReportService(factory.patients(), factory.appointments(), factory.bills(), factory.users());
        this.dentistService = new DentistService(factory.dentists());
    }

    public AuthService auth() {
        return authService;
    }

    public StaffService staff() {
        return staffService;
    }

    public PatientService patients() {
        return patientService;
    }

    public AppointmentService appointments() {
        return appointmentService;
    }

    public BillingService billing() {
        return billingService;
    }

    public ReportService reports() {
        return reportService;
    }

    public DentistService dentists() {
        return dentistService;
    }
}
