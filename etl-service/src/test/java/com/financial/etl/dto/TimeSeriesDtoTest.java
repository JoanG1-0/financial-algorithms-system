package com.financial.etl.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeSeriesDtoTest {

    @Test
    void shouldGetAndSetAllTimeSeriesMetaFields() {
        TimeSeriesMeta meta = new TimeSeriesMeta();
        meta.setSymbol("AAPL");
        meta.setInterval("1day");
        meta.setCurrency("USD");
        meta.setExchangeTimezone("America/New_York");
        meta.setExchange("NASDAQ");
        meta.setMicCode("XNMS");
        meta.setType("Common Stock");

        assertEquals("AAPL", meta.getSymbol());
        assertEquals("1day", meta.getInterval());
        assertEquals("USD", meta.getCurrency());
        assertEquals("America/New_York", meta.getExchangeTimezone());
        assertEquals("NASDAQ", meta.getExchange());
        assertEquals("XNMS", meta.getMicCode());
        assertEquals("Common Stock", meta.getType());
    }

    @Test
    void shouldGetAndSetAllTimeSeriesValueFields() {
        TimeSeriesValue value = new TimeSeriesValue();
        value.setDatetime("2024-01-05");
        value.setOpen("181.99");
        value.setHigh("182.76");
        value.setLow("180.17");
        value.setClose("181.18");
        value.setVolume("62371161");

        assertEquals("2024-01-05", value.getDatetime());
        assertEquals("181.99", value.getOpen());
        assertEquals("182.76", value.getHigh());
        assertEquals("180.17", value.getLow());
        assertEquals("181.18", value.getClose());
        assertEquals("62371161", value.getVolume());
    }

    @Test
    void shouldGetAndSetAllTimeSeriesResponseFields() {
        TimeSeriesMeta meta = new TimeSeriesMeta();
        meta.setSymbol("AAPL");

        TimeSeriesValue value = new TimeSeriesValue();
        value.setDatetime("2024-01-05");
        List<TimeSeriesValue> values = List.of(value);

        TimeSeriesResponse response = new TimeSeriesResponse();
        response.setMeta(meta);
        response.setValues(values);
        response.setStatus("ok");
        response.setMessage("success");

        assertEquals(meta, response.getMeta());
        assertEquals(values, response.getValues());
        assertEquals("ok", response.getStatus());
        assertEquals("success", response.getMessage());
    }
}
