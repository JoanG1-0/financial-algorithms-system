package com.financial.etl.controller;

import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.service.EtlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
}
