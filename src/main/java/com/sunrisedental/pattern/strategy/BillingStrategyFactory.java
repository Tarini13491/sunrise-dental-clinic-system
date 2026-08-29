package com.sunrisedental.pattern.strategy;

/**
 * Selects the billing strategy from the treatment category stored in MySQL.
 */
public final class BillingStrategyFactory {

    private BillingStrategyFactory() {
    }

    public static BillingStrategy forCategory(String category) {
        if (category == null) {
            return new StandardBillingStrategy();
        }
        return switch (category.toUpperCase()) {
            case "COSMETIC", "ORTHODONTIC" -> new CosmeticBillingStrategy();
            case "PEDIATRIC" -> new PediatricBillingStrategy();
            case "EMERGENCY" -> new EmergencyBillingStrategy();
            default -> new StandardBillingStrategy();
        };
    }
}
