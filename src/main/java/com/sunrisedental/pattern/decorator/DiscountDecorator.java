package com.sunrisedental.pattern.decorator;

import java.math.BigDecimal;

public class DiscountDecorator extends BillDecorator {

    private final BigDecimal discount;

    public DiscountDecorator(BillComponent inner, BigDecimal discount) {
        super(inner);
        this.discount = discount == null ? BigDecimal.ZERO : discount;
    }

    @Override
    public String description() {
        if (discount.signum() == 0) {
            return inner.description();
        }
        return inner.description() + " − discount";
    }

    @Override
    public BigDecimal amount() {
        BigDecimal next = inner.amount().subtract(discount);
        return next.signum() < 0 ? BigDecimal.ZERO : next;
    }
}
