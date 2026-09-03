package com.sunrise.clinic.billing;

import com.sunrise.clinic.model.Treatment;

import java.math.BigDecimal;

public final class BillAmounts {
    private final BigDecimal treatmentCost;
    private final BigDecimal consultationFee;
    private final BigDecimal totalAmount;

    public BillAmounts(BigDecimal treatmentCost, BigDecimal consultationFee, BigDecimal totalAmount) {
        this.treatmentCost = treatmentCost;
        this.consultationFee = consultationFee;
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTreatmentCost() {
        return treatmentCost;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getTreatmentName(Treatment treatment) {
        return treatment.getName();
    }
}
