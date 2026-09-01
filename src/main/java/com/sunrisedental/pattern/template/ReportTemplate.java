package com.sunrisedental.pattern.template;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ReportTemplate {

    public final Map<String, Object> generate() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("title", title());
        report.put("purpose", purpose());
        List<Map<String, Object>> rows = fetchRows();
        report.put("rows", rows);
        report.put("summary", summarise(rows));
        return report;
    }

    protected abstract String title();

    protected abstract String purpose();

    protected abstract List<Map<String, Object>> fetchRows();

    protected abstract Map<String, Object> summarise(List<Map<String, Object>> rows);
}
