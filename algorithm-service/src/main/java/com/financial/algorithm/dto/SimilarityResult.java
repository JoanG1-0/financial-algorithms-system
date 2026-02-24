package com.financial.algorithm.dto;

/**
 * DTO que encapsula los cuatro índices de similitud calculados entre dos activos.
 *
 * <p>Los algoritmos de similitud operan sobre retornos diarios normalizados,
 * no sobre precios crudos, para comparar activos con escalas de precio distintas.
 */
public class SimilarityResult {

    private String tickerA;
    private String tickerB;
    private double euclidean;
    private double pearson;
    private double dtw;
    private double cosine;

    public SimilarityResult() {}

    private SimilarityResult(Builder builder) {
        this.tickerA = builder.tickerA;
        this.tickerB = builder.tickerB;
        this.euclidean = builder.euclidean;
        this.pearson = builder.pearson;
        this.dtw = builder.dtw;
        this.cosine = builder.cosine;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTickerA() { return tickerA; }
    public void setTickerA(String tickerA) { this.tickerA = tickerA; }

    public String getTickerB() { return tickerB; }
    public void setTickerB(String tickerB) { this.tickerB = tickerB; }

    public double getEuclidean() { return euclidean; }
    public void setEuclidean(double euclidean) { this.euclidean = euclidean; }

    public double getPearson() { return pearson; }
    public void setPearson(double pearson) { this.pearson = pearson; }

    public double getDtw() { return dtw; }
    public void setDtw(double dtw) { this.dtw = dtw; }

    public double getCosine() { return cosine; }
    public void setCosine(double cosine) { this.cosine = cosine; }

    public static class Builder {
        private String tickerA;
        private String tickerB;
        private double euclidean;
        private double pearson;
        private double dtw;
        private double cosine;

        public Builder tickerA(String tickerA) { this.tickerA = tickerA; return this; }
        public Builder tickerB(String tickerB) { this.tickerB = tickerB; return this; }
        public Builder euclidean(double euclidean) { this.euclidean = euclidean; return this; }
        public Builder pearson(double pearson) { this.pearson = pearson; return this; }
        public Builder dtw(double dtw) { this.dtw = dtw; return this; }
        public Builder cosine(double cosine) { this.cosine = cosine; return this; }

        public SimilarityResult build() {
            return new SimilarityResult(this);
        }
    }
}
