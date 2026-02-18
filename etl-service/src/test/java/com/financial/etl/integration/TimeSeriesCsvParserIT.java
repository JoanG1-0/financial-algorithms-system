package com.financial.etl.integration;

import com.financial.etl.client.TwelveDataHttpClient;
import com.financial.etl.parser.TimeSeriesCsvParser;
import com.financial.etl.repository.FinancialSeriesRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
    }
)
@Tag("integration")
class TimeSeriesCsvParserIT {

    @MockBean
    private FinancialSeriesRepository financialSeriesRepository;

    @Autowired
    private TwelveDataHttpClient client;

    @Autowired
    private TimeSeriesCsvParser parser;

    @Test
    void shouldParseRealApiResponseToCsv() {

        String json = client.downloadTimeSeries("AAPL");

        String csv = parser.convertToCsv(json);

        assertNotNull(csv, "El CSV no debe ser nulo");

        String[] lines = csv.split("\n");
        assertTrue(lines.length > 1, "El CSV debe contener al menos el header y una fila de datos");
        assertEquals("datetime,open,high,low,close,volume", lines[0], "El header del CSV debe ser correcto");

        for (int i = 1; i < lines.length; i++) {
            String[] columns = lines[i].split(",");
            assertEquals(6, columns.length, "Cada fila debe tener 6 columnas, fila: " + i);
        }
    }
}
