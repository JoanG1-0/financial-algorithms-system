package com.financial.algorithm.dto;

import java.util.List;

/**
 * Resultado agregado de la detección de un patrón sobre un activo.
 *
 * <p>Incluye el conteo total de ocurrencias, la frecuencia relativa
 * (ocurrencias / ventanas totales) y la lista detallada de ocurrencias.
 */
public class PatternResult {

    private String symbol;
    private String patternType;
    private int totalWindows;
    private int occurrences;
    private double relativeFrequency;
    private List<PatternOccurrence> details;

    public PatternResult() {}

    private PatternResult(Builder builder) {
        this.symbol = builder.symbol;
        this.patternType = builder.patternType;
        this.totalWindows = builder.totalWindows;
        this.occurrences = builder.occurrences;
        this.relativeFrequency = builder.relativeFrequency;
        this.details = builder.details;
    }

    public static Builder builder() { return new Builder(); }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) { this.patternType = patternType; }

    public int getTotalWindows() { return totalWindows; }
    public void setTotalWindows(int totalWindows) { this.totalWindows = totalWindows; }

    public int getOccurrences() { return occurrences; }
    public void setOccurrences(int occurrences) { this.occurrences = occurrences; }

    public double getRelativeFrequency() { return relativeFrequency; }
    public void setRelativeFrequency(double relativeFrequency) { this.relativeFrequency = relativeFrequency; }

    public List<PatternOccurrence> getDetails() { return details; }
    public void setDetails(List<PatternOccurrence> details) { this.details = details; }

    public static class Builder {
        private String symbol;
        private String patternType;
        private int totalWindows;
        private int occurrences;
        private double relativeFrequency;
        private List<PatternOccurrence> details;

        public Builder symbol(String symbol) { this.symbol = symbol; return this; }
        public Builder patternType(String patternType) { this.patternType = patternType; return this; }
        public Builder totalWindows(int totalWindows) { this.totalWindows = totalWindows; return this; }
        public Builder occurrences(int occurrences) { this.occurrences = occurrences; return this; }
        public Builder relativeFrequency(double relativeFrequency) { this.relativeFrequency = relativeFrequency; return this; }
        public Builder details(List<PatternOccurrence> details) { this.details = details; return this; }

        public PatternResult build() { return new PatternResult(this); }
    }
}
