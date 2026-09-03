package com.sunrise.clinic.catalog;

import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.Treatment;

import java.math.BigDecimal;
import java.util.List;

public final class TreatmentCatalog {
    public static final BigDecimal CONSULTATION_FEE = new BigDecimal("1500.00");

    private static final List<Treatment> TREATMENTS = List.of(
            new Treatment("Consultation", new BigDecimal("0.00")),
            new Treatment("Scaling and Polishing", new BigDecimal("4500.00")),
            new Treatment("Tooth Filling", new BigDecimal("6000.00")),
            new Treatment("Tooth Extraction", new BigDecimal("5000.00")),
            new Treatment("Root Canal Treatment", new BigDecimal("18000.00")),
            new Treatment("Dental Crown", new BigDecimal("25000.00")),
            new Treatment("Teeth Whitening", new BigDecimal("12000.00")),
            new Treatment("Dental X-Ray", new BigDecimal("2500.00"))
    );

    private TreatmentCatalog() {
    }

    public static List<Treatment> all() {
        return TREATMENTS;
    }

    public static Treatment require(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Treatment type is required.");
        }
        String trimmed = name.trim();
        return TREATMENTS.stream()
                .filter(treatment -> treatment.getName().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Select a valid treatment type."));
    }
}
