package com.financial.etl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSeriesMeta {

    private String symbol;
    private String interval;
    private String currency;

    @JsonProperty("exchange_timezone")
    private String exchangeTimezone;

    private String exchange;

    @JsonProperty("mic_code")
    private String micCode;

    private String type;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getExchangeTimezone() {
        return exchangeTimezone;
    }

    public void setExchangeTimezone(String exchangeTimezone) {
        this.exchangeTimezone = exchangeTimezone;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getMicCode() {
        return micCode;
    }

    public void setMicCode(String micCode) {
        this.micCode = micCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
