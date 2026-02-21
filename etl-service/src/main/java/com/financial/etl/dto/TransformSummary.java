package com.financial.etl.dto;

import java.time.LocalDateTime;

public class TransformSummary {

    private int symbolsProcessed;
    private int calendarDays;
    private long totalRecords;
    private long cleanRecords;
    private long forwardFilledRecords;
    private long anomalyCorrectedRecords;
    private long anomalyFlaggedRecords;
    private LocalDateTime transformedAt;

    public TransformSummary() {
    }

    public TransformSummary(int symbolsProcessed, int calendarDays, long totalRecords,
                             long cleanRecords, long forwardFilledRecords,
                             long anomalyCorrectedRecords, long anomalyFlaggedRecords,
                             LocalDateTime transformedAt) {
        this.symbolsProcessed = symbolsProcessed;
        this.calendarDays = calendarDays;
        this.totalRecords = totalRecords;
        this.cleanRecords = cleanRecords;
        this.forwardFilledRecords = forwardFilledRecords;
        this.anomalyCorrectedRecords = anomalyCorrectedRecords;
        this.anomalyFlaggedRecords = anomalyFlaggedRecords;
        this.transformedAt = transformedAt;
    }

    public int getSymbolsProcessed() { return symbolsProcessed; }
    public void setSymbolsProcessed(int symbolsProcessed) { this.symbolsProcessed = symbolsProcessed; }

    public int getCalendarDays() { return calendarDays; }
    public void setCalendarDays(int calendarDays) { this.calendarDays = calendarDays; }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public long getCleanRecords() { return cleanRecords; }
    public void setCleanRecords(long cleanRecords) { this.cleanRecords = cleanRecords; }

    public long getForwardFilledRecords() { return forwardFilledRecords; }
    public void setForwardFilledRecords(long forwardFilledRecords) { this.forwardFilledRecords = forwardFilledRecords; }

    public long getAnomalyCorrectedRecords() { return anomalyCorrectedRecords; }
    public void setAnomalyCorrectedRecords(long anomalyCorrectedRecords) { this.anomalyCorrectedRecords = anomalyCorrectedRecords; }

    public long getAnomalyFlaggedRecords() { return anomalyFlaggedRecords; }
    public void setAnomalyFlaggedRecords(long anomalyFlaggedRecords) { this.anomalyFlaggedRecords = anomalyFlaggedRecords; }

    public LocalDateTime getTransformedAt() { return transformedAt; }
    public void setTransformedAt(LocalDateTime transformedAt) { this.transformedAt = transformedAt; }
}
