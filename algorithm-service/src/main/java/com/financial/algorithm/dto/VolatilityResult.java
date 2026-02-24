package com.financial.algorithm.dto;

/**
 * Resultado del cálculo de volatilidad histórica de un activo.
 *
 * <p>Contiene los retornos logarítmicos calculados, la desviación estándar
 * diaria y la volatilidad anualizada (factor sqrt(252)).
 */
public class VolatilityResult {

    private double[] logReturns;
    private double dailySigma;
    private double annualizedSigma;

    public VolatilityResult() {}

    private VolatilityResult(Builder builder) {
        this.logReturns = builder.logReturns;
        this.dailySigma = builder.dailySigma;
        this.annualizedSigma = builder.annualizedSigma;
    }

    public static Builder builder() { return new Builder(); }

    public double[] getLogReturns() { return logReturns; }
    public void setLogReturns(double[] logReturns) { this.logReturns = logReturns; }

    public double getDailySigma() { return dailySigma; }
    public void setDailySigma(double dailySigma) { this.dailySigma = dailySigma; }

    public double getAnnualizedSigma() { return annualizedSigma; }
    public void setAnnualizedSigma(double annualizedSigma) { this.annualizedSigma = annualizedSigma; }

    public static class Builder {
        private double[] logReturns;
        private double dailySigma;
        private double annualizedSigma;

        public Builder logReturns(double[] logReturns) { this.logReturns = logReturns; return this; }
        public Builder dailySigma(double dailySigma) { this.dailySigma = dailySigma; return this; }
        public Builder annualizedSigma(double annualizedSigma) { this.annualizedSigma = annualizedSigma; return this; }

        public VolatilityResult build() { return new VolatilityResult(this); }
    }
}
