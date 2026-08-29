package com.sunrisedental.service;

import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.Bill;
import com.sunrisedental.pattern.facade.ClinicFacade;
import com.sunrisedental.pattern.factory.DaoFactory;
import com.sunrisedental.pattern.observer.ClinicEvent;
import com.sunrisedental.pattern.observer.NotificationPublisher;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class BillingService {

    private final ClinicFacade facade = new ClinicFacade();

    public Map<String, Object> preview(String appointmentNumber) {
        Appointment appointment = DaoFactory.get().appointments().findByNumber(appointmentNumber);
        if (appointment == null) {
            return Map.of("success", false, "message", "No appointment found for that number.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "Bill preview ready.");
        result.put("appointment", appointment);
        result.put("preview", facade.preview(appointment, BigDecimal.ZERO));
        Bill existing = DaoFactory.get().bills().findByAppointmentNumber(appointmentNumber);
        if (existing != null) {
            result.put("bill", existing);
            result.put("message", "A bill already exists for this appointment.");
        }
        return result;
    }

    public Map<String, Object> calculate(String appointmentNumber, BigDecimal extraDiscount) {
        if (appointmentNumber == null || appointmentNumber.isBlank()) {
            return Map.of("success", false, "message", "Enter the appointment number first.");
        }
        return facade.issueBill(appointmentNumber.trim().toUpperCase(), extraDiscount);
    }

    public Map<String, Object> pay(String billNumber, String method, BigDecimal amount) {
        if (billNumber == null || billNumber.isBlank()) {
            return Map.of("success", false, "message", "Bill number is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            return Map.of("success", false, "message", "Enter the amount received.");
        }
        String message = DaoFactory.get().bills().markPaid(billNumber, method == null ? "CASH" : method, amount);
        Bill bill = DaoFactory.get().bills().findByNumber(billNumber);
        if (bill != null && "PAID".equals(bill.getPaymentStatus())) {
            Appointment appointment = DaoFactory.get().appointments().findByNumber(bill.getAppointmentNumber());
            if (appointment != null) {
                NotificationPublisher.get().publish(ClinicEvent.PAYMENT_RECEIVED, appointment, billNumber);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put("bill", bill);
        return result;
    }

    public Bill find(String billNumber) {
        return DaoFactory.get().bills().findByNumber(billNumber);
    }
}
