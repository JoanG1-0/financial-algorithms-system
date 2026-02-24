package com.financial.report.dto;

import java.time.LocalDateTime;

/**
 * DTO que mapea la respuesta del endpoint GET /api/algorithm/patterns del algorithm-service.
 */
public class PatternResponse {

    private String symbol;
    private String patternType;
    private int occurrences;
    private double relativeFrequency;
    private LocalDateTime computedAt;

    public PatternResponse() {
        // Required for JSON deserialization: Jackson needs a public no-arg constructor to instantiate DTOs via reflection.
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) { this.patternType = patternType; }

    public int getOccurrences() { return occurrences; }
    public void setOccurrences(int occurrences) { this.occurrences = occurrences; }

    public double getRelativeFrequency() { return relativeFrequency; }
    public void setRelativeFrequency(double relativeFrequency) { this.relativeFrequency = relativeFrequency; }

    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
}
