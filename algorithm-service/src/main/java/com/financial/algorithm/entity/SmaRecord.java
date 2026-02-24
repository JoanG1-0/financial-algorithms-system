package com.financial.algorithm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que persiste los valores SMA calculados para un activo.
 *
 * <p>El array de valores SMA se serializa como JSON en {@code valuesJson}
 * para simplificar el esquema de la base de datos.
 */
@Entity
@Table(name = "sma_results")
public class SmaRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "sma_window")
    private int window;

    @Column(columnDefinition = "TEXT")
    private String valuesJson;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    public SmaRecord() {
        // Required by JPA: Hibernate needs a public no-arg constructor to instantiate entities via reflection.
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getWindow() { return window; }
    public void setWindow(int window) { this.window = window; }

    public String getValuesJson() { return valuesJson; }
    public void setValuesJson(String valuesJson) { this.valuesJson = valuesJson; }

    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
}
