package com.sunrisedental.pattern.decorator;

import java.math.BigDecimal;

public class BaseBill implements BillComponent {

    private final BigDecimal consultation;
    private final BigDecimal treatment;

    public BaseBill(BigDecimal consultation, BigDecimal treatment) {
        this.consultation = consultation;
        this.treatment = treatment;
    }

    @Override
    public String description() {
        return "Consultation + treatment";
    }

    @Override
    public BigDecimal amount() {
        return consultation.add(treatment);
    }
}
