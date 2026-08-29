package com.sunrisedental.pattern.strategy;

import java.math.BigDecimal;

/** Cosmetic treatments are priced as listed — no clinical subsidy. */
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
