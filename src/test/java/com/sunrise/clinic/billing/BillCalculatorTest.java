package com.sunrise.clinic.billing;

import com.sunrise.clinic.catalog.TreatmentCatalog;
import com.sunrise.clinic.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BillCalculatorTest {
    private final BillCalculator calculator = new BillCalculator();

    @Test
    void totalsTreatmentCostAndConsultationFee() {
        BillAmounts filling = calculator.calculate(TreatmentCatalog.require("Tooth Filling"));
        assertEquals(new BigDecimal("6000.00"), filling.getTreatmentCost());
        assertEquals(new BigDecimal("1500.00"), filling.getConsultationFee());
        assertEquals(new BigDecimal("7500.00"), filling.getTotalAmount());
    }

    @Test
    void consultationOnlyStillAddsTheConsultationFee() {
        BillAmounts consultation = calculator.calculate(TreatmentCatalog.require("Consultation"));
        assertEquals(new BigDecimal("0.00"), consultation.getTreatmentCost());
        assertEquals(new BigDecimal("1500.00"), consultation.getTotalAmount());
    }

    @Test
    void rejectsUnknownTreatments() {
        assertThrows(ValidationException.class, () -> TreatmentCatalog.require("Unknown"));
        assertThrows(ValidationException.class, () -> calculator.calculate(null));
    }
}
