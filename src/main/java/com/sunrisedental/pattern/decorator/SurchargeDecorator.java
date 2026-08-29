package com.sunrisedental.pattern.decorator;

import java.math.BigDecimal;

public class SurchargeDecorator extends BillDecorator {

    private final BigDecimal surcharge;

    public SurchargeDecorator(BillComponent inner, BigDecimal surcharge) {
        super(inner);
        this.surcharge = surcharge == null ? BigDecimal.ZERO : surcharge;
    }

    @Override
    public String description() {
        if (surcharge.signum() == 0) {
            return inner.description();
        }
        return inner.description() + " + emergency surcharge";
    }

    @Override
    public BigDecimal amount() {
        return inner.amount().add(surcharge);
    }
}
