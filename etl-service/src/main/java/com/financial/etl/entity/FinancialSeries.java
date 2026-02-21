package com.financial.etl.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "financial_series")
public class FinancialSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    @Column(name = "series_interval")
    private String interval;

    private String currency;
    private String exchange;

    @Column(name = "exchange_timezone")
    private String exchangeTimezone;

    @Column(name = "mic_code")
    private String micCode;

    private String type;

    @Column(name = "loaded_at")
    private LocalDateTime loadedAt;

    @Column(name = "batch_id")
    private Long batchId;

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PriceRecord> priceRecords = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.loadedAt = LocalDateTime.now();
    }
}
