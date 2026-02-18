package com.financial.etl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.etl.client.TwelveDataHttpClient;
import com.financial.etl.dto.TimeSeriesMeta;
import com.financial.etl.dto.TimeSeriesResponse;
import com.financial.etl.dto.TimeSeriesValue;
import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.repository.FinancialSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtlServiceTest {

    @Mock
    private TwelveDataHttpClient httpClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FinancialSeriesRepository repository;

    @InjectMocks
    private EtlService etlService;

    private TimeSeriesResponse sampleResponse;

    @BeforeEach
    void setUp() {
        TimeSeriesMeta meta = new TimeSeriesMeta();
        meta.setSymbol("AAPL");
        meta.setInterval("1day");
        meta.setCurrency("USD");
        meta.setExchange("NASDAQ");
        meta.setExchangeTimezone("America/New_York");
        meta.setMicCode("XNGS");
        meta.setType("Common Stock");

        TimeSeriesValue value = new TimeSeriesValue();
        value.setDatetime("2024-01-02");
        value.setOpen("185.0000");
        value.setHigh("188.0000");
        value.setLow("184.5000");
        value.setClose("187.1500");
        value.setVolume("55312400");

        sampleResponse = new TimeSeriesResponse();
        sampleResponse.setMeta(meta);
        sampleResponse.setValues(List.of(value));
    }

    @Test
    void extractAndLoad_callsClientParsesAndSaves() throws Exception {
        String rawJson = "{\"meta\":{},\"values\":[]}";
        when(httpClient.downloadTimeSeries("AAPL")).thenReturn(rawJson);
        when(objectMapper.readValue(eq(rawJson), eq(TimeSeriesResponse.class))).thenReturn(sampleResponse);

        FinancialSeries saved = new FinancialSeries();
        saved.setId(1L);
        saved.setSymbol("AAPL");
        when(repository.save(any(FinancialSeries.class))).thenReturn(saved);

        FinancialSeries result = etlService.extractAndLoad("AAPL");

        verify(httpClient).downloadTimeSeries("AAPL");
        verify(objectMapper).readValue(eq(rawJson), eq(TimeSeriesResponse.class));
        verify(repository).save(any(FinancialSeries.class));
        assertThat(result.getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void findBySymbol_delegatesToRepository() {
        FinancialSeries series = new FinancialSeries();
        series.setSymbol("AAPL");
        when(repository.findBySymbol("AAPL")).thenReturn(List.of(series));

        List<FinancialSeries> result = etlService.findBySymbol("AAPL");

        verify(repository).findBySymbol("AAPL");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
    }
}
