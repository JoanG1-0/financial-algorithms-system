package com.financial.etl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cleaned_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "date"}))
public class CleanedRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private LocalDate date;

    @Column(precision = 18, scale = 6)
    private BigDecimal open;

    @Column(precision = 18, scale = 6)
    private BigDecimal high;

    @Column(precision = 18, scale = 6)
    private BigDecimal low;

    @Column(precision = 18, scale = 6)
    private BigDecimal close;

    private Long volume;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_quality", length = 30, nullable = false)
    private DataQuality dataQuality;

    @Column(name = "anomaly_type", length = 100)
    private String anomalyType;

    @Column(name = "original_close", precision = 18, scale = 6)
    private BigDecimal originalClose;

    @Column(name = "transformed_at", nullable = false)
    private LocalDateTime transformedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }

    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }

    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }

    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }

    public DataQuality getDataQuality() { return dataQuality; }
    public void setDataQuality(DataQuality dataQuality) { this.dataQuality = dataQuality; }

    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }

    public BigDecimal getOriginalClose() { return originalClose; }
    public void setOriginalClose(BigDecimal originalClose) { this.originalClose = originalClose; }

    public LocalDateTime getTransformedAt() { return transformedAt; }
    public void setTransformedAt(LocalDateTime transformedAt) { this.transformedAt = transformedAt; }
}
