package com.financial.algorithm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que persiste el perfil de riesgo de un activo.
 *
 * <p>Almacena la volatilidad anualizada y la categoría de riesgo asignada
 * (CONSERVATIVE, MODERATE, AGGRESSIVE).
 */
@Entity
@Table(name = "risk_profiles")
public class RiskRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    private double annualizedVolatility;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    public RiskRecord() {
        // Required by JPA: Hibernate needs a public no-arg constructor to instantiate entities via reflection.
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public double getAnnualizedVolatility() { return annualizedVolatility; }
    public void setAnnualizedVolatility(double annualizedVolatility) { this.annualizedVolatility = annualizedVolatility; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
}
