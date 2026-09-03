package com.sunrise.clinic.billing;

import com.sunrise.clinic.catalog.TreatmentCatalog;
import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.Treatment;

import java.math.BigDecimal;

public final class StandardBillingFormula implements BillingFormula {
    @Override
    public BillAmounts calculate(Treatment treatment) {
        if (treatment == null) {
            throw new ValidationException("Treatment type is required to calculate a bill.");
        }
        BigDecimal treatmentCost = treatment.getCost();
        BigDecimal consultationFee = TreatmentCatalog.CONSULTATION_FEE;
        if (treatmentCost.compareTo(BigDecimal.ZERO) < 0 || consultationFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Billing amounts cannot be negative.");
        }
        return new BillAmounts(treatmentCost, consultationFee, treatmentCost.add(consultationFee));
    }
}
