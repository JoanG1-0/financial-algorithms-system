package com.financial.algorithm.dto;

/**
 * Perfil de riesgo de un activo tras clasificación por volatilidad anualizada.
 *
 * <p>Categorías:
 * <ul>
 *   <li>CONSERVATIVE: σ_anual &lt; 0.15</li>
 *   <li>MODERATE: 0.15 ≤ σ_anual ≤ 0.30</li>
 *   <li>AGGRESSIVE: σ_anual &gt; 0.30</li>
 * </ul>
 */
public class RiskProfile {

    /** Categoría de riesgo según volatilidad anualizada. */
    public enum RiskCategory { CONSERVATIVE, MODERATE, AGGRESSIVE }

    private String ticker;
    private double annualizedVolatility;
    private RiskCategory category;

    public RiskProfile() {}

    private RiskProfile(Builder builder) {
        this.ticker = builder.ticker;
        this.annualizedVolatility = builder.annualizedVolatility;
        this.category = builder.category;
    }

    public static Builder builder() { return new Builder(); }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public double getAnnualizedVolatility() { return annualizedVolatility; }
    public void setAnnualizedVolatility(double annualizedVolatility) { this.annualizedVolatility = annualizedVolatility; }

    public RiskCategory getCategory() { return category; }
    public void setCategory(RiskCategory category) { this.category = category; }

    public static class Builder {
        private String ticker;
        private double annualizedVolatility;
        private RiskCategory category;

        public Builder ticker(String ticker) { this.ticker = ticker; return this; }
        public Builder annualizedVolatility(double annualizedVolatility) { this.annualizedVolatility = annualizedVolatility; return this; }
        public Builder category(RiskCategory category) { this.category = category; return this; }

        public RiskProfile build() { return new RiskProfile(this); }
    }
}
