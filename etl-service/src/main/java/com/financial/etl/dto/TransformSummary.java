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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int symbolsProcessed;
        private int calendarDays;
        private long totalRecords;
        private long cleanRecords;
        private long forwardFilledRecords;
        private long anomalyCorrectedRecords;
        private long anomalyFlaggedRecords;
        private LocalDateTime transformedAt;

        public Builder symbolsProcessed(int val) { this.symbolsProcessed = val; return this; }
        public Builder calendarDays(int val) { this.calendarDays = val; return this; }
        public Builder totalRecords(long val) { this.totalRecords = val; return this; }
        public Builder cleanRecords(long val) { this.cleanRecords = val; return this; }
        public Builder forwardFilledRecords(long val) { this.forwardFilledRecords = val; return this; }
        public Builder anomalyCorrectedRecords(long val) { this.anomalyCorrectedRecords = val; return this; }
        public Builder anomalyFlaggedRecords(long val) { this.anomalyFlaggedRecords = val; return this; }
        public Builder transformedAt(LocalDateTime val) { this.transformedAt = val; return this; }

        public TransformSummary build() {
            TransformSummary s = new TransformSummary();
            s.symbolsProcessed = this.symbolsProcessed;
            s.calendarDays = this.calendarDays;
            s.totalRecords = this.totalRecords;
            s.cleanRecords = this.cleanRecords;
            s.forwardFilledRecords = this.forwardFilledRecords;
            s.anomalyCorrectedRecords = this.anomalyCorrectedRecords;
            s.anomalyFlaggedRecords = this.anomalyFlaggedRecords;
            s.transformedAt = this.transformedAt;
            return s;
        }
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
