package com.sunrisedental.pattern.decorator;

import java.math.BigDecimal;

public abstract class BillDecorator implements BillComponent {

    protected final BillComponent inner;

    protected BillDecorator(BillComponent inner) {
        this.inner = inner;
    }
}
