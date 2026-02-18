package com.financial.etl.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.etl.exception.JsonParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeSeriesCsvParserTest {

    private TimeSeriesCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new TimeSeriesCsvParser(new ObjectMapper());
    }

    @Test
    void shouldConvertValidJsonToCsv() {
        String json = """
                {
                  "meta": { "symbol": "AAPL", "interval": "1day", "currency": "USD" },
                  "values": [
                    { "datetime": "2024-01-05", "open": "181.99", "high": "182.76", "low": "180.17", "close": "181.18", "volume": "62371161" },
                    { "datetime": "2024-01-04", "open": "182.15", "high": "183.09", "low": "180.89", "close": "181.91", "volume": "71983584" }
                  ]
                }
                """;

        String csv = parser.convertToCsv(json);

        String[] lines = csv.split("\n");
        assertEquals(3, lines.length);
        assertEquals("2024-01-05,181.99,182.76,180.17,181.18,62371161", lines[1]);
        assertEquals("2024-01-04,182.15,183.09,180.89,181.91,71983584", lines[2]);
    }

    @Test
    void shouldIncludeHeaderRow() {
        String json = """
                {
                  "meta": { "symbol": "AAPL" },
                  "values": [
                    { "datetime": "2024-01-05", "open": "181.99", "high": "182.76", "low": "180.17", "close": "181.18", "volume": "62371161" }
                  ]
                }
                """;

        String csv = parser.convertToCsv(json);

        assertTrue(csv.startsWith("datetime,open,high,low,close,volume"));
    }

    @Test
    void shouldThrowExceptionForInvalidJson() {
        String invalidJson = "esto no es json";

        assertThrows(JsonParsingException.class, () -> parser.convertToCsv(invalidJson));
    }

    @Test
    void shouldHandleEmptyValues() {
        String json = """
                {
                  "meta": { "symbol": "AAPL" },
                  "values": []
                }
                """;

        String csv = parser.convertToCsv(json);

        assertEquals("datetime,open,high,low,close,volume", csv);
    }

    @Test
    void shouldHandleNullValuesField() {
        String json = """
                {
                  "meta": { "symbol": "AAPL" }
                }
                """;

        String csv = parser.convertToCsv(json);

        assertEquals("datetime,open,high,low,close,volume", csv);
    }
}
