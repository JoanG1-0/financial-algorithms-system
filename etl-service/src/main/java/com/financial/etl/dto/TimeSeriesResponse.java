package com.financial.etl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSeriesResponse {

    private TimeSeriesMeta meta;
    private List<TimeSeriesValue> values;

    public TimeSeriesMeta getMeta() {
        return meta;
    }

    public void setMeta(TimeSeriesMeta meta) {
        this.meta = meta;
    }

    public List<TimeSeriesValue> getValues() {
        return values;
    }

    public void setValues(List<TimeSeriesValue> values) {
        this.values = values;
    }
}
