package com.sunrisedental.pattern.strategy;

import java.math.BigDecimal;

public class StandardBillingStrategy implements BillingStrategy {
    @Override
    public String name() {
        return "Standard clinical billing";
    }

    @Override
    public BigDecimal treatmentMultiplier() {
        return BigDecimal.ONE;
    }

    @Override
    public BigDecimal discountRate() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal surchargeRate() {
        return BigDecimal.ZERO;
    }
}
