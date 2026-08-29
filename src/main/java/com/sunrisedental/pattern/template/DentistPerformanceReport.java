package com.sunrisedental.pattern.template;

import com.sunrisedental.pattern.factory.DaoFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DentistPerformanceReport extends ReportTemplate {

    @Override
    protected String title() {
        return "Dentist workload and revenue";
    }

    @Override
    protected String purpose() {
        return "Helps the practice manager balance chairs and review each clinician's collections.";
    }

    @Override
    protected List<Map<String, Object>> fetchRows() {
        return DaoFactory.get().reports().dentistPerformance();
    }

    @Override
    protected Map<String, Object> summarise(List<Map<String, Object>> rows) {
        BigDecimal revenue = BigDecimal.ZERO;
        int appointments = 0;
        for (Map<String, Object> row : rows) {
            revenue = revenue.add(asDecimal(row.get("revenue")));
            appointments += asInt(row.get("appointment_count"));
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("dentists", rows.size());
        summary.put("appointments", appointments);
        summary.put("revenue", revenue);
        return summary;
    }

    private BigDecimal asDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private int asInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}
