package com.financial.report.dto;

import java.time.LocalDateTime;

/**
 * DTO que mapea la respuesta del endpoint GET /api/algorithm/similarity del algorithm-service.
 */
public class SimilarityResponse {

    private String tickerA;
    private String tickerB;
    private double euclidean;
    private double pearson;
    private double dtw;
    private double cosine;
    private LocalDateTime computedAt;

    public SimilarityResponse() {
        // Required for JSON deserialization: Jackson needs a public no-arg constructor to instantiate DTOs via reflection.
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

    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
}
