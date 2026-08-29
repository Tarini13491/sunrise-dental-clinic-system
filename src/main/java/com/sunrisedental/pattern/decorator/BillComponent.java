package com.sunrisedental.pattern.decorator;

import java.math.BigDecimal;

/**
 * Decorator pattern — bills are built by wrapping a base amount with tax,
 * discounts, and surcharges.
 *
 * Why: the assignment asks for a bill made from treatment cost + consultation
 * fee, plus extras. Decorator lets us add those extras independently and still
 * print a single total. Strategy chooses *which* extras apply; Decorator
 * applies them to the running total.
 *
 * Evaluation: Decorator shines when extras can be stacked (tax after discount).
 * It is heavier than a single calculate() method, but it makes the receipt
 * line-items obvious in both code and the printed bill.
 */
public interface BillComponent {
    String description();

    BigDecimal amount();
}
