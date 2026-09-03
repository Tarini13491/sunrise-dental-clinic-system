package com.sunrise.clinic.billing;

import com.sunrise.clinic.model.Treatment;

public final class BillCalculator {
    private final BillingFormula formula;

    public BillCalculator() {
        this(BillingFormulaFactory.standard());
    }

    public BillCalculator(BillingFormula formula) {
        this.formula = formula;
    }

    public BillAmounts calculate(Treatment treatment) {
        return formula.calculate(treatment);
    }
}
