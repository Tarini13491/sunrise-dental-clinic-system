package com.sunrisedental.pattern.decorator;

import com.sunrisedental.config.AppConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TaxDecorator extends BillDecorator {

    private final BigDecimal rate;

    public TaxDecorator(BillComponent inner) {
        super(inner);
        this.rate = BigDecimal.valueOf(AppConfig.getDouble("clinic.vat.rate", 0.08));
    }

    public BigDecimal taxAmount() {
        return inner.amount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String description() {
        return inner.description() + " + VAT";
    }

    @Override
    public BigDecimal amount() {
        return inner.amount().add(taxAmount());
    }
}
