package com.financial.etl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
