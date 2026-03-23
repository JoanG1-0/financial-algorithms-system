package com.financial.etl.dto;

import com.financial.etl.entity.CleanedRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CleanedRecordDto {

    private String symbol;
    private LocalDate date;
    private BigDecimal close;
    private Long volume;

    public CleanedRecordDto(CleanedRecord r) {
        this.symbol = r.getSymbol();
        this.date   = r.getDate();
        this.close  = r.getClose();
        this.volume = r.getVolume();
    }

    public String     getSymbol() { return symbol; }
    public LocalDate  getDate()   { return date; }
    public BigDecimal getClose()  { return close; }
    public Long       getVolume() { return volume; }
}
