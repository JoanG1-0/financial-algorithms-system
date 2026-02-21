package com.financial.etl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.etl.client.TwelveDataHttpClient;
import com.financial.etl.dto.TimeSeriesMeta;
import com.financial.etl.dto.TimeSeriesResponse;
import com.financial.etl.dto.TimeSeriesValue;
import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.exception.DataDownloadException;
import com.financial.etl.exception.JsonParsingException;
import com.financial.etl.repository.FinancialSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        when(objectMapper.readValue(rawJson, TimeSeriesResponse.class)).thenReturn(sampleResponse);
        when(repository.findFirstBySymbol("AAPL")).thenReturn(Optional.empty());

        FinancialSeries saved = new FinancialSeries();
        saved.setId(1L);
        saved.setSymbol("AAPL");
        when(repository.save(any(FinancialSeries.class))).thenReturn(saved);

        FinancialSeries result = etlService.extractAndLoad("AAPL");

        verify(httpClient).downloadTimeSeries("AAPL");
        verify(objectMapper).readValue(rawJson, TimeSeriesResponse.class);
        verify(repository).save(any(FinancialSeries.class));
        assertThat(result.getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void extractAndLoad_updatesExistingSeries_whenSymbolAlreadyExists() throws Exception {
        String rawJson = "{\"meta\":{},\"values\":[]}";
        when(httpClient.downloadTimeSeries("AAPL")).thenReturn(rawJson);
        when(objectMapper.readValue(rawJson, TimeSeriesResponse.class)).thenReturn(sampleResponse);

        FinancialSeries existing = new FinancialSeries();
        existing.setId(42L);
        existing.setSymbol("AAPL");
        existing.setBatchId(1L);
        when(repository.findFirstBySymbol("AAPL")).thenReturn(Optional.of(existing));

        FinancialSeries saved = new FinancialSeries();
        saved.setId(42L);
        saved.setSymbol("AAPL");
        when(repository.save(any(FinancialSeries.class))).thenReturn(saved);

        FinancialSeries result = etlService.extractAndLoad("AAPL", 2L);

        verify(repository).save(argThat(s -> s.getId() != null && s.getId().equals(42L)));
        assertThat(result.getId()).isEqualTo(42L);
    }

    @Test
    void extractAndLoad_throwsJsonParsingException_whenObjectMapperFails() throws Exception {
        String rawJson = "invalid-json";
        when(httpClient.downloadTimeSeries("AAPL")).thenReturn(rawJson);
        when(objectMapper.readValue(rawJson, TimeSeriesResponse.class))
                .thenThrow(new JsonProcessingException("parse error") {});

        assertThatThrownBy(() -> etlService.extractAndLoad("AAPL"))
                .isInstanceOf(JsonParsingException.class)
                .hasMessageContaining("AAPL");

        verify(repository, never()).save(any());
    }

    @Test
    void extractAndLoad_throwsDataDownloadException_whenMetaIsNullWithMessage() throws Exception {
        String rawJson = "{\"message\":\"API limit exceeded\"}";
        TimeSeriesResponse errorResponse = new TimeSeriesResponse();
        errorResponse.setMeta(null);
        errorResponse.setMessage("API limit exceeded");

        when(httpClient.downloadTimeSeries("AAPL")).thenReturn(rawJson);
        when(objectMapper.readValue(rawJson, TimeSeriesResponse.class)).thenReturn(errorResponse);

        assertThatThrownBy(() -> etlService.extractAndLoad("AAPL"))
                .isInstanceOf(DataDownloadException.class)
                .hasMessageContaining("API limit exceeded");

        verify(repository, never()).save(any());
    }

    @Test
    void extractAndLoad_throwsDataDownloadException_whenMetaIsNullWithoutMessage() throws Exception {
        String rawJson = "{}";
        TimeSeriesResponse errorResponse = new TimeSeriesResponse();
        errorResponse.setMeta(null);
        errorResponse.setMessage(null);

        when(httpClient.downloadTimeSeries("AAPL")).thenReturn(rawJson);
        when(objectMapper.readValue(rawJson, TimeSeriesResponse.class)).thenReturn(errorResponse);

        assertThatThrownBy(() -> etlService.extractAndLoad("AAPL"))
                .isInstanceOf(DataDownloadException.class)
                .hasMessageContaining("respuesta inesperada de la API");

        verify(repository, never()).save(any());
    }

    @Test
    void extractAndLoad_handlesNullResponseValues() throws Exception {
        String rawJson = "{\"meta\":{}}";
        TimeSeriesResponse responseWithNullValues = new TimeSeriesResponse();
        responseWithNullValues.setMeta(sampleResponse.getMeta());
        responseWithNullValues.setValues(null);

        when(httpClient.downloadTimeSeries("AAPL")).thenReturn(rawJson);
        when(objectMapper.readValue(rawJson, TimeSeriesResponse.class)).thenReturn(responseWithNullValues);
        when(repository.findFirstBySymbol("AAPL")).thenReturn(Optional.empty());

        FinancialSeries saved = new FinancialSeries();
        saved.setSymbol("AAPL");
        when(repository.save(any(FinancialSeries.class))).thenReturn(saved);

        FinancialSeries result = etlService.extractAndLoad("AAPL");

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
