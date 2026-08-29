package com.sunrisedental.pattern.facade;

import com.sunrisedental.dao.AppointmentDao;
import com.sunrisedental.dao.BillDao;
import com.sunrisedental.model.Appointment;
import com.sunrisedental.model.Bill;
import com.sunrisedental.pattern.decorator.BaseBill;
import com.sunrisedental.pattern.decorator.BillComponent;
import com.sunrisedental.pattern.decorator.DiscountDecorator;
import com.sunrisedental.pattern.decorator.SurchargeDecorator;
import com.sunrisedental.pattern.decorator.TaxDecorator;
import com.sunrisedental.pattern.factory.DaoFactory;
import com.sunrisedental.pattern.observer.ClinicEvent;
import com.sunrisedental.pattern.observer.NotificationPublisher;
import com.sunrisedental.pattern.strategy.BillingStrategy;
import com.sunrisedental.pattern.strategy.BillingStrategyFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Facade pattern — one entry point for "book a visit" and "issue a bill".
 *
 * Why: a servlet should not orchestrate stored procedures, billing strategy,
 * decorator stack and observers. The facade is the business-tier API.
 *
 * Evaluation: Facade is the backbone of the 3-tier split. Services remain
 * thin wrappers; the facade is where use-cases live. The risk is a "god"
 * facade — we keep it to the two clinic use-cases that span multiple DAOs.
 */
public class ClinicFacade {

    private final AppointmentDao appointments = DaoFactory.get().appointments();
    private final BillDao bills = DaoFactory.get().bills();

    public Map<String, Object> registerAppointment(String patientName, String address, String contact,
                                                   String email, int dentistId, int treatmentId,
                                                   Date date, Time time, String notes, Integer userId) {
        Map<String, Object> raw = appointments.registerViaProcedure(
                patientName, address, contact, email, dentistId, treatmentId, date, time, notes, userId);
        String number = (String) raw.get("appointmentNumber");
        String message = (String) raw.get("message");
        Map<String, Object> result = new LinkedHashMap<>(raw);
        result.put("success", number != null && !number.isBlank());
        if (number != null) {
            Appointment created = appointments.findByNumber(number);
            if (created != null) {
                NotificationPublisher.get().publish(ClinicEvent.APPOINTMENT_BOOKED, created, null);
                result.put("appointment", created);
            }
        }
        result.put("message", message);
        return result;
    }

    public Map<String, Object> issueBill(String appointmentNumber, BigDecimal extraDiscount) {
        Appointment appointment = appointments.findByNumber(appointmentNumber);
        if (appointment == null) {
            return Map.of("success", false, "message", "No appointment found for that number.");
        }
        if (appointment.getBillNumber() != null) {
            Bill existing = bills.findByNumber(appointment.getBillNumber());
            return Map.of("success", true, "message", "A bill already exists for this appointment.",
                    "bill", existing, "preview", preview(appointment, extraDiscount));
        }

        BillingStrategy strategy = BillingStrategyFactory.forCategory(appointment.getTreatmentCategory());
        BigDecimal discount = strategy.discount(appointment);
        if (extraDiscount != null) {
            discount = discount.add(extraDiscount);
        }
        BigDecimal surcharge = strategy.surcharge(appointment);

        Map<String, Object> raw = bills.calculateViaProcedure(appointment.getAppointmentId(), discount, surcharge);
        String billNumber = (String) raw.get("billNumber");
        boolean ok = billNumber != null && !billNumber.isBlank();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", ok);
        result.put("message", raw.get("message"));
        result.put("strategy", strategy.name());
        result.put("preview", preview(appointment, extraDiscount));
        if (ok) {
            Bill bill = bills.findByNumber(billNumber);
            result.put("bill", bill);
            Appointment updated = appointments.findByNumber(appointmentNumber);
            NotificationPublisher.get().publish(ClinicEvent.BILL_ISSUED, updated, billNumber);
        }
        return result;
    }

    public Map<String, Object> preview(Appointment appointment, BigDecimal extraDiscount) {
        BillingStrategy strategy = BillingStrategyFactory.forCategory(appointment.getTreatmentCategory());
        BigDecimal treatment = strategy.treatmentCost(appointment);
        BigDecimal discount = strategy.discount(appointment);
        if (extraDiscount != null) {
            discount = discount.add(extraDiscount);
        }
        BigDecimal surcharge = strategy.surcharge(appointment);
        BillComponent stack = new BaseBill(appointment.getConsultationFee(), treatment);
        stack = new SurchargeDecorator(stack, surcharge);
        stack = new DiscountDecorator(stack, discount);
        TaxDecorator taxed = new TaxDecorator(stack);

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("strategy", strategy.name());
        preview.put("consultationFee", appointment.getConsultationFee());
        preview.put("treatmentCost", treatment);
        preview.put("surcharge", surcharge);
        preview.put("discount", discount);
        preview.put("tax", taxed.taxAmount());
        preview.put("total", taxed.amount());
        preview.put("description", taxed.description());
        return preview;
    }
}
