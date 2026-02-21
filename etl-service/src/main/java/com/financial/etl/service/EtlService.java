package com.financial.etl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.etl.client.TwelveDataHttpClient;
import com.financial.etl.dto.TimeSeriesMeta;
import com.financial.etl.dto.TimeSeriesResponse;
import com.financial.etl.dto.TimeSeriesValue;
import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.entity.PriceRecord;
import com.financial.etl.exception.DataDownloadException;
import com.financial.etl.exception.JsonParsingException;
import com.financial.etl.repository.FinancialSeriesRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class EtlService {

    private final TwelveDataHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final FinancialSeriesRepository repository;
    private final EtlService self;

    public EtlService(TwelveDataHttpClient httpClient,
                      ObjectMapper objectMapper,
                      FinancialSeriesRepository repository,
                      EtlService self) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.self = self;
    }

    @Transactional
    public FinancialSeries extractAndLoad(String symbol) {
        return self.extractAndLoad(symbol, null);
    }

    @Transactional
    public FinancialSeries extractAndLoad(String symbol, Long batchId) {
        String json = httpClient.downloadTimeSeries(symbol);

        TimeSeriesResponse response;
        try {
            response = objectMapper.readValue(json, TimeSeriesResponse.class);
        } catch (JsonProcessingException e) {
            throw new JsonParsingException("Error al parsear la respuesta JSON para: " + symbol, e);
        }

        TimeSeriesMeta meta = response.getMeta();
        if (meta == null) {
            String apiMessage = response.getMessage() != null ? response.getMessage() : "respuesta inesperada de la API";
            throw new DataDownloadException("TwelveData rechazó la solicitud para '" + symbol + "': " + apiMessage, null);
        }

        FinancialSeries series = new FinancialSeries();
        series.setSymbol(meta.getSymbol());
        series.setInterval(meta.getInterval());
        series.setCurrency(meta.getCurrency());
        series.setExchange(meta.getExchange());
        series.setExchangeTimezone(meta.getExchangeTimezone());
        series.setMicCode(meta.getMicCode());
        series.setType(meta.getType());
        series.setBatchId(batchId);

        if (response.getValues() != null) {
            for (TimeSeriesValue v : response.getValues()) {
                PriceRecord priceRecord = new PriceRecord();
                priceRecord.setDatetime(LocalDate.parse(v.getDatetime()));
                priceRecord.setOpen(new BigDecimal(v.getOpen()));
                priceRecord.setHigh(new BigDecimal(v.getHigh()));
                priceRecord.setLow(new BigDecimal(v.getLow()));
                priceRecord.setClose(new BigDecimal(v.getClose()));
                priceRecord.setVolume(Long.parseLong(v.getVolume()));
                priceRecord.setSeries(series);
                series.getPriceRecords().add(priceRecord);
            }
        }

        return repository.save(series);
    }

    @Transactional(readOnly = true)
    public List<FinancialSeries> findBySymbol(String symbol) {
        return repository.findBySymbol(symbol);
    }
}
