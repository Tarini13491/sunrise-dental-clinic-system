package com.sunrisedental.service;

import com.sunrisedental.model.DashboardStats;
import com.sunrisedental.pattern.factory.DaoFactory;
import com.sunrisedental.pattern.template.DailyClinicReport;
import com.sunrisedental.pattern.template.DentistPerformanceReport;
import com.sunrisedental.pattern.template.MonthlyRevenueReport;
import com.sunrisedental.pattern.template.TreatmentPopularityReport;

import java.time.LocalDate;
import java.util.Map;

public class ReportService {

    public DashboardStats dashboard() {
        DashboardStats stats = DaoFactory.get().reports().dashboard();
        stats.setUpcoming(DaoFactory.get().appointments().listUpcoming(8));
        stats.setTreatmentMix(DaoFactory.get().reports().treatmentPopularity());
        stats.setDentistLoad(DaoFactory.get().reports().dentistPerformance());
        return stats;
    }

    public Map<String, Object> report(String type, LocalDate date) {
        return switch (type == null ? "daily" : type) {
            case "dentists" -> new DentistPerformanceReport().generate();
            case "treatments" -> new TreatmentPopularityReport().generate();
            case "revenue" -> new MonthlyRevenueReport().generate();
            default -> new DailyClinicReport(date == null ? LocalDate.now() : date).generate();
        };
    }
}
