package com.sunrise.clinic.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class Treatment {
    private final String name;
    private final BigDecimal cost;

    public Treatment(String name, BigDecimal cost) {
        this.name = name;
        this.cost = cost;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCost() {
        return cost;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Treatment treatment)) {
            return false;
        }
        return name.equalsIgnoreCase(treatment.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }
}
