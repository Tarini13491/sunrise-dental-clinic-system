package com.sunrisedental.pattern.strategy;

import java.math.BigDecimal;

public class EmergencyBillingStrategy implements BillingStrategy {
    @Override
    public String name() {
        return "Emergency surcharge";
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
        return new BigDecimal("0.20");
    }
}
