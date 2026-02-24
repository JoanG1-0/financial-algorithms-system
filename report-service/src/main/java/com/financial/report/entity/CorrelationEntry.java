package com.financial.report.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que persiste una entrada de la matriz de correlación de Pearson.
 *
 * <p>Almacena la correlación de Pearson entre dos activos (assetA, assetB),
 * incluyendo la diagonal (assetA == assetB, correlation == 1.0).
 */
@Entity
@Table(name = "correlation_matrix")
public class CorrelationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String assetA;

    @Column(nullable = false)
    private String assetB;

    @Column(nullable = false)
    private double correlation;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    public CorrelationEntry() {
        // Required by JPA: Hibernate needs a public no-arg constructor to instantiate entities via reflection.
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAssetA() { return assetA; }
    public void setAssetA(String assetA) { this.assetA = assetA; }

    public String getAssetB() { return assetB; }
    public void setAssetB(String assetB) { this.assetB = assetB; }

    public double getCorrelation() { return correlation; }
    public void setCorrelation(double correlation) { this.correlation = correlation; }

    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
}
