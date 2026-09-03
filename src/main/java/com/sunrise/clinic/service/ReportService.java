package com.sunrise.clinic.service;

import com.sunrise.clinic.dao.AppointmentDao;
import com.sunrise.clinic.dao.BillDao;
import com.sunrise.clinic.dao.PatientDao;
import com.sunrise.clinic.dao.UserDao;
import com.sunrise.clinic.model.BillRecord;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.security.AccessPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportService {
    private final PatientDao patients;
    private final AppointmentDao appointments;
    private final BillDao bills;
    private final UserDao users;

    public ReportService(PatientDao patients, AppointmentDao appointments, BillDao bills, UserDao users) {
        this.patients = patients;
        this.appointments = appointments;
        this.bills = bills;
        this.users = users;
    }

    public Map<String, Object> summary(SessionUser actor) {
        AccessPolicy.requireUser(actor);
        Map<String, Object> summary = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        summary.put("patientCount", patients.findAll().size());
        summary.put("appointmentCount", appointments.findAll().size());
        summary.put("todayAppointmentCount", appointments.countForDate(today));
        summary.put("billCount", bills.findAll().size());
        summary.put("todayBillCount", bills.countForDate(today));
        summary.put("staffCount", users.findStaffMembers().size());
        BigDecimal revenue = bills.findAll().stream()
                .map(BillRecord::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("totalRevenue", revenue);
        return summary;
    }
}
