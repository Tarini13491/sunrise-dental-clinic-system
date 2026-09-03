package com.sunrise.clinic.billing;

import com.sunrise.clinic.model.Treatment;

public interface BillingFormula {
    BillAmounts calculate(Treatment treatment);
}
