package com.sunrisedental.pattern.template;

import com.sunrisedental.pattern.factory.DaoFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyClinicReport extends ReportTemplate {

    private final Date date;

    public DailyClinicReport(LocalDate date) {
        this.date = Date.valueOf(date);
    }

    @Override
    protected String title() {
        return "Daily clinic summary — " + date;
    }

    @Override
    protected String purpose() {
        return "Front-desk snapshot of today's bookings, completions and collections.";
    }

    @Override
    protected List<Map<String, Object>> fetchRows() {
        List<Map<String, Object>> summary = DaoFactory.get().reports().dailySummary(date);
        if (summary.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("appointment_date", date);
            empty.put("total_appointments", 0);
            empty.put("billed_total", BigDecimal.ZERO);
            empty.put("collected_total", BigDecimal.ZERO);
            summary.add(empty);
        }
        return summary;
    }

    @Override
    protected Map<String, Object> summarise(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? Map.of("note", "No appointments on this date.") : rows.get(0);
    }
}
