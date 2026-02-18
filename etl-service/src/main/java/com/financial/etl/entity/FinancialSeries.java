package com.financial.etl.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PriceRecord> priceRecords = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.loadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getInterval() { return interval; }
    public void setInterval(String interval) { this.interval = interval; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getExchangeTimezone() { return exchangeTimezone; }
    public void setExchangeTimezone(String exchangeTimezone) { this.exchangeTimezone = exchangeTimezone; }

    public String getMicCode() { return micCode; }
    public void setMicCode(String micCode) { this.micCode = micCode; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getLoadedAt() { return loadedAt; }
    public void setLoadedAt(LocalDateTime loadedAt) { this.loadedAt = loadedAt; }

    public List<PriceRecord> getPriceRecords() { return priceRecords; }
    public void setPriceRecords(List<PriceRecord> priceRecords) { this.priceRecords = priceRecords; }
}
