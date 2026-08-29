package com.sunrisedental.pattern.template;

import com.sunrisedental.pattern.factory.DaoFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreatmentPopularityReport extends ReportTemplate {

    @Override
    protected String title() {
        return "Treatment popularity";
    }

    @Override
    protected String purpose() {
        return "Shows which treatments drive visits so the clinic can stock materials and plan campaigns.";
    }

    @Override
    protected List<Map<String, Object>> fetchRows() {
        return DaoFactory.get().reports().treatmentPopularity();
    }

    @Override
    protected Map<String, Object> summarise(List<Map<String, Object>> rows) {
        String top = rows.isEmpty() ? "—" : String.valueOf(rows.get(0).get("treatment_name"));
        Map<String, Object> summary = new HashMap<>();
        summary.put("treatments", rows.size());
        summary.put("mostBooked", top);
        return summary;
    }
}
