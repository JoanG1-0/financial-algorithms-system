package com.financial.algorithm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que persiste el resultado de similitud entre dos activos.
 *
 * <p>Almacena los cuatro índices de similitud (Euclidean, Pearson, DTW, Cosine)
 * calculados sobre los retornos diarios de los tickers A y B.
 */
@Entity
@Table(name = "similarity_results")
public class SimilarityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tickerA;

    @Column(nullable = false)
    private String tickerB;

    private double euclidean;
    private double pearson;
    private double dtw;
    private double cosine;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    public SimilarityRecord() {
        // Required by JPA: Hibernate needs a public no-arg constructor to instantiate entities via reflection.
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
