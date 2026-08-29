package com.sunrisedental.pattern.template;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Template Method pattern — every management report follows the same
 * skeleton: title, fetch, summarise, ready-for-JSON.
 *
 * Why: daily revenue, dentist performance and treatment popularity all need
 * the same wrapping for the dashboard. Subclasses only supply the query.
 *
 * Evaluation: Template Method prevents copy-paste in the report servlets.
 * It is less flexible than Strategy if report steps start to diverge a lot;
 * for this clinic the steps are stable.
 */
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
