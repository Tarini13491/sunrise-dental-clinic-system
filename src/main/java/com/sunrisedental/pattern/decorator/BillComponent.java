package com.sunrisedental.pattern.decorator;

import java.math.BigDecimal;

public interface BillComponent {
    String description();

    BigDecimal amount();
}
