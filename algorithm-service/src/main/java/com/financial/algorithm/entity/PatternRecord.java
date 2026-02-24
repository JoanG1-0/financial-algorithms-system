package com.financial.algorithm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que persiste el resultado de detección de un patrón sobre un activo.
 *
 * <p>Los índices de ocurrencia se serializan como JSON en el campo {@code indicesJson}
 * para evitar una tabla adicional de detalles.
 */
@Entity
@Table(name = "pattern_results")
public class PatternRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String patternType;

    private int occurrences;
    private double relativeFrequency;

    @Column(columnDefinition = "TEXT")
    private String indicesJson;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    public PatternRecord() {
        // Required by JPA: Hibernate needs a public no-arg constructor to instantiate entities via reflection.
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) { this.patternType = patternType; }

    public int getOccurrences() { return occurrences; }
    public void setOccurrences(int occurrences) { this.occurrences = occurrences; }

    public double getRelativeFrequency() { return relativeFrequency; }
    public void setRelativeFrequency(double relativeFrequency) { this.relativeFrequency = relativeFrequency; }

    public String getIndicesJson() { return indicesJson; }
    public void setIndicesJson(String indicesJson) { this.indicesJson = indicesJson; }

    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
}
