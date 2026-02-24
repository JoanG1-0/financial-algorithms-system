package com.financial.report.dto;

import java.time.LocalDateTime;

/**
 * DTO que mapea la respuesta del endpoint GET /api/algorithm/risk del algorithm-service.
 */
public class RiskResponse {

    private String ticker;
    private double annualizedVolatility;
    private String category;
    private LocalDateTime computedAt;

    public RiskResponse() {
        // Required for JSON deserialization: Jackson needs a public no-arg constructor to instantiate DTOs via reflection.
    }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public double getAnnualizedVolatility() { return annualizedVolatility; }
    public void setAnnualizedVolatility(double annualizedVolatility) { this.annualizedVolatility = annualizedVolatility; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
}
