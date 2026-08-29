package com.sunrisedental.pattern.strategy;

import com.sunrisedental.model.Appointment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Strategy pattern — billing rules differ by treatment category.
 *
 * Why: consultation + treatment cost is not always a simple sum. Cosmetic
 * work has no clinical discount, paediatric visits apply a family discount,
 * and emergencies carry a surcharge. Encoding this as if/else in a servlet
 * would mix presentation with business rules.
 *
 * Evaluation: Strategy keeps each rule in its own class and lets the factory
 * pick one at runtime. The trade-off is more classes; that is justified here
 * because the assignment requires a critical evaluation of pattern impact.
 */
public interface BillingStrategy {

    String name();

    BigDecimal treatmentMultiplier();

    BigDecimal discountRate();

    BigDecimal surchargeRate();

    default BigDecimal treatmentCost(Appointment appointment) {
        return appointment.getTreatmentCost()
                .multiply(treatmentMultiplier())
                .setScale(2, RoundingMode.HALF_UP);
    }

    default BigDecimal discount(Appointment appointment) {
        BigDecimal base = appointment.getConsultationFee().add(treatmentCost(appointment));
        return base.multiply(discountRate()).setScale(2, RoundingMode.HALF_UP);
    }

    default BigDecimal surcharge(Appointment appointment) {
        return treatmentCost(appointment).multiply(surchargeRate()).setScale(2, RoundingMode.HALF_UP);
    }
}
