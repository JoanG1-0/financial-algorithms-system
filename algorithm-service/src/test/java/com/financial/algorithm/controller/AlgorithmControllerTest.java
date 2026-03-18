package com.financial.algorithm.controller;

import com.financial.algorithm.dto.DashboardSummary;
import com.financial.algorithm.entity.PatternRecord;
import com.financial.algorithm.entity.RiskRecord;
import com.financial.algorithm.entity.SimilarityRecord;
import com.financial.algorithm.entity.SmaRecord;
import com.financial.algorithm.service.AlgorithmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlgorithmController.class)
class AlgorithmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlgorithmService algorithmService;

    @Test
    void run_returns202WithSummary() throws Exception {
        AlgorithmService.AnalysisSummary summary =
                new AlgorithmService.AnalysisSummary(10, 5, 20, 5);
        when(algorithmService.runFullAnalysis()).thenReturn(summary);

        mockMvc.perform(post("/api/algorithm/run"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.similarityPairs").value(10))
                .andExpect(jsonPath("$.riskProfiles").value(5))
                .andExpect(jsonPath("$.patternRecords").value(20))
                .andExpect(jsonPath("$.smaRecords").value(5));
    }

    @Test
    void similarity_returns200WithList() throws Exception {
        SimilarityRecord rec = new SimilarityRecord();
        rec.setTickerA("AAA");
        rec.setTickerB("BBB");
        rec.setComputedAt(LocalDateTime.now());
        when(algorithmService.getSimilarityResults()).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/algorithm/similarity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tickerA").value("AAA"))
                .andExpect(jsonPath("$[0].tickerB").value("BBB"));
    }

    @Test
    void risk_returns200WithList() throws Exception {
        RiskRecord rec = new RiskRecord();
        rec.setTicker("VOO");
        rec.setCategory("MODERATE");
        rec.setComputedAt(LocalDateTime.now());
        when(algorithmService.getRiskResults()).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/algorithm/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("VOO"))
                .andExpect(jsonPath("$[0].category").value("MODERATE"));
    }

    @Test
    void patterns_returns200WhenFound() throws Exception {
        PatternRecord rec = new PatternRecord();
        rec.setSymbol("AAPL");
        rec.setPatternType("CONSECUTIVE_UP");
        rec.setComputedAt(LocalDateTime.now());
        when(algorithmService.getPatternsBySymbol("AAPL")).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/algorithm/patterns/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].patternType").value("CONSECUTIVE_UP"));
    }

    @Test
    void patterns_returns404WhenNotFound() throws Exception {
        when(algorithmService.getPatternsBySymbol("UNKNOWN")).thenReturn(List.of());

        mockMvc.perform(get("/api/algorithm/patterns/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sma_returns200WhenFound() throws Exception {
        SmaRecord rec = new SmaRecord();
        rec.setSymbol("TSLA");
        rec.setWindow(20);
        rec.setValuesJson("[1.0,2.0,3.0]");
        rec.setComputedAt(LocalDateTime.now());
        when(algorithmService.getSma("TSLA", 20)).thenReturn(rec);

        mockMvc.perform(get("/api/algorithm/indicators/TSLA/sma?window=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("TSLA"))
                .andExpect(jsonPath("$.window").value(20));
    }

    @Test
    void sma_returns404WhenNotFound() throws Exception {
        when(algorithmService.getSma("UNKNOWN", 20)).thenReturn(null);

        mockMvc.perform(get("/api/algorithm/indicators/UNKNOWN/sma"))
                .andExpect(status().isNotFound());
    }

    @Test
    void allPatterns_returns200WithList() throws Exception {
        PatternRecord rec = new PatternRecord();
        rec.setSymbol("AAPL");
        rec.setPatternType("CONSECUTIVE_UP");
        rec.setOccurrences(5);
        rec.setComputedAt(LocalDateTime.now());
        when(algorithmService.getAllPatterns()).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/algorithm/patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].patternType").value("CONSECUTIVE_UP"));
    }

    @Test
    void dashboardSummary_returns200WithSummary() throws Exception {
        RiskRecord top = new RiskRecord();
        top.setTicker("TSLA");
        top.setAnnualizedVolatility(0.58);
        top.setCategory("AGGRESSIVE");
        top.setComputedAt(LocalDateTime.now());

        RiskRecord low = new RiskRecord();
        low.setTicker("KO");
        low.setAnnualizedVolatility(0.18);
        low.setCategory("CONSERVATIVE");
        low.setComputedAt(LocalDateTime.now());

        DashboardSummary summary = new DashboardSummary(
                20, 190L, LocalDateTime.now(),
                java.util.Map.of("CONSERVATIVE", 7L, "MODERATE", 9L, "AGGRESSIVE", 4L),
                top, low
        );
        when(algorithmService.getDashboardSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/algorithm/dashboard-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssets").value(20))
                .andExpect(jsonPath("$.totalSimilarityPairs").value(190))
                .andExpect(jsonPath("$.topRiskAsset.ticker").value("TSLA"))
                .andExpect(jsonPath("$.lowestRiskAsset.ticker").value("KO"));
    }
}
