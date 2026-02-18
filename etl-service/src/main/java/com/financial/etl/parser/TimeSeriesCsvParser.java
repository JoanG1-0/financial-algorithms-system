package com.financial.etl.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.etl.dto.TimeSeriesResponse;
import com.financial.etl.dto.TimeSeriesValue;
import com.financial.etl.exception.JsonParsingException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TimeSeriesCsvParser {

    private static final String CSV_HEADER = "datetime,open,high,low,close,volume";

    private final ObjectMapper objectMapper;

    public TimeSeriesCsvParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String convertToCsv(String json) {
        try {
            TimeSeriesResponse response = objectMapper.readValue(json, TimeSeriesResponse.class);
            return buildCsv(response.getValues());
        } catch (JsonParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonParsingException("Error al parsear el JSON de la serie de tiempo", e);
        }
    }

    private String buildCsv(List<TimeSeriesValue> values) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER);

        if (values != null) {
            for (TimeSeriesValue v : values) {
                sb.append("\n");
                sb.append(v.getDatetime()).append(",");
                sb.append(v.getOpen()).append(",");
                sb.append(v.getHigh()).append(",");
                sb.append(v.getLow()).append(",");
                sb.append(v.getClose()).append(",");
                sb.append(v.getVolume());
            }
        }

        return sb.toString();
    }
}
