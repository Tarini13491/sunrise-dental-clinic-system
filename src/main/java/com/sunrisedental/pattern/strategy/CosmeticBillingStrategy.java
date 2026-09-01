package com.sunrisedental.pattern.strategy;

import java.math.BigDecimal;

public class CosmeticBillingStrategy implements BillingStrategy {
    @Override
    public String name() {
        return "Cosmetic billing";
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
