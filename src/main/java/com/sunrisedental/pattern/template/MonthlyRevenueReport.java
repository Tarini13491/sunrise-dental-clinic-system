package com.sunrisedental.pattern.template;

import com.sunrisedental.pattern.factory.DaoFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonthlyRevenueReport extends ReportTemplate {

    @Override
    protected String title() {
        return "Monthly revenue";
    }

    @Override
    protected String purpose() {
        return "Decision report for cash-flow: billed versus collected by month.";
    }

    @Override
    protected List<Map<String, Object>> fetchRows() {
        return DaoFactory.get().reports().monthlyRevenue();
    }

    @Override
    protected Map<String, Object> summarise(List<Map<String, Object>> rows) {
        BigDecimal billed = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            billed = billed.add(asDecimal(row.get("total_revenue")));
            collected = collected.add(asDecimal(row.get("collected")));
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("months", rows.size());
        summary.put("billed", billed);
        summary.put("collected", collected);
        return summary;
    }

    private BigDecimal asDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
