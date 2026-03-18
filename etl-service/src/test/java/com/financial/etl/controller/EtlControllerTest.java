package com.financial.etl.controller;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.entity.PriceRecord;
import com.financial.etl.service.EtlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EtlController.class)
class EtlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EtlService etlService;

    @Test
    void postExtract_returns201() throws Exception {
        FinancialSeries saved = new FinancialSeries();
        saved.setId(1L);
        saved.setSymbol("AAPL");
        when(etlService.extractAndLoad("AAPL")).thenReturn(saved);

        mockMvc.perform(post("/api/etl/extract")
                        .param("symbol", "AAPL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    void getSeries_returns200WithList() throws Exception {
        FinancialSeries series = new FinancialSeries();
        series.setId(1L);
        series.setSymbol("AAPL");
        when(etlService.findBySymbol("AAPL")).thenReturn(List.of(series));

        mockMvc.perform(get("/api/etl/series/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    @Test
    void getAllAssets_returns200WithList() throws Exception {
        FinancialSeries s1 = new FinancialSeries();
        s1.setSymbol("AAPL");
        s1.setExchange("NASDAQ");
        FinancialSeries s2 = new FinancialSeries();
        s2.setSymbol("MSFT");
        s2.setExchange("NASDAQ");
        when(etlService.findAllAssets()).thenReturn(List.of(s1, s2));

        mockMvc.perform(get("/api/etl/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[1].symbol").value("MSFT"));
    }

    @Test
    void getPrices_returns200WithPage() throws Exception {
        PriceRecord pr = new PriceRecord();
        pr.setDatetime(LocalDate.of(2026, 3, 18));
        pr.setClose(new BigDecimal("171.30"));
        when(etlService.findPricesBySymbol(eq("AAPL"), any()))
                .thenReturn(new PageImpl<>(List.of(pr), PageRequest.of(0, 30), 1));

        mockMvc.perform(get("/api/etl/series/AAPL/prices?page=0&size=30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].close").value(171.30))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getCleaned_returns200WithPage() throws Exception {
        CleanedRecord cr = new CleanedRecord();
        cr.setSymbol("AAPL");
        cr.setDate(LocalDate.of(2026, 3, 18));
        cr.setClose(new BigDecimal("171.30"));
        cr.setDataQuality(DataQuality.CLEAN);
        cr.setTransformedAt(LocalDateTime.now());
        when(etlService.findCleanedBySymbol(eq("AAPL"), any()))
                .thenReturn(new PageImpl<>(List.of(cr), PageRequest.of(0, 30), 1));

        mockMvc.perform(get("/api/etl/series/AAPL/cleaned?page=0&size=30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.content[0].dataQuality").value("CLEAN"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
