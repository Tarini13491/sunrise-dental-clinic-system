package com.sunrisedental.pattern.strategy;

import com.sunrisedental.model.Appointment;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
