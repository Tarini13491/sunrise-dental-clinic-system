package com.sunrisedental.pattern.strategy;

import java.math.BigDecimal;

public class PediatricBillingStrategy implements BillingStrategy {
    @Override
    public String name() {
        return "Pediatric family discount";
    }

    @Override
    public BigDecimal treatmentMultiplier() {
        return BigDecimal.ONE;
    }

    @Override
    public BigDecimal discountRate() {
        return new BigDecimal("0.10");
    }

    @Override
    public BigDecimal surchargeRate() {
        return BigDecimal.ZERO;
    }
}
