package com.sunrise.clinic.billing;

public final class BillingFormulaFactory {
    private BillingFormulaFactory() {
    }

    public static BillingFormula standard() {
        return new StandardBillingFormula();
    }
}
