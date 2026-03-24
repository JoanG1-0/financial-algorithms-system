package com.financial.etl.controller;

import com.financial.etl.dto.BenchmarkSummaryDto;
import com.financial.etl.dto.CleanedRecordDto;
import com.financial.etl.dto.SortingResultDto;
import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import com.financial.etl.sorting.SortingBenchmark;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SortingController.class)
class SortingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SortingBenchmark benchmark;

    // -----------------------------------------------------------------------
    // GET /api/etl/sort/benchmark
    // -----------------------------------------------------------------------

    @Test
    void benchmark_returns200WithSummary() throws Exception {
        SortingResultDto result = new SortingResultDto("TimSort", 1000, 0.005);
        BenchmarkSummaryDto summary = new BenchmarkSummaryDto(
                List.of(result),
                List.of(result),
                List.of(sampleDto())
        );
        when(benchmark.run()).thenReturn(summary);

        mockMvc.perform(get("/api/etl/sort/benchmark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortByDateClose[0].algorithm").value("TimSort"))
                .andExpect(jsonPath("$.sortByDateClose[0].size").value(1000))
                .andExpect(jsonPath("$.sortByVolume[0].algorithm").value("TimSort"))
                .andExpect(jsonPath("$.top15ByVolume[0].symbol").value("AAPL"));
    }

    @Test
    void benchmark_returnsEmptyListsWhenNoData() throws Exception {
        BenchmarkSummaryDto summary = new BenchmarkSummaryDto(List.of(), List.of(), List.of());
        when(benchmark.run()).thenReturn(summary);

        mockMvc.perform(get("/api/etl/sort/benchmark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortByDateClose").isEmpty())
                .andExpect(jsonPath("$.sortByVolume").isEmpty())
                .andExpect(jsonPath("$.top15ByVolume").isEmpty());
    }

    // -----------------------------------------------------------------------
    // GET /api/etl/sort/top15
    // -----------------------------------------------------------------------

    @Test
    void top15_returns200WithList() throws Exception {
        when(benchmark.runTop15()).thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/etl/sort/top15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].volume").value(9000));
    }

    @Test
    void top15_returnsEmptyListWhenNoData() throws Exception {
        when(benchmark.runTop15()).thenReturn(List.of());

        mockMvc.perform(get("/api/etl/sort/top15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // -----------------------------------------------------------------------
    // GET /api/etl/sort/result
    // -----------------------------------------------------------------------

    @Test
    void result_returns200WithSortedRecords() throws Exception {
        when(benchmark.sortedSample("TimSort", "date", 20))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/etl/sort/result")
                        .param("algorithm", "TimSort")
                        .param("criterion", "date")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].volume").value(9000));
    }

    @Test
    void result_usesDefaultCriterionAndLimit() throws Exception {
        when(benchmark.sortedSample("HeapSort", "date", 20))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/etl/sort/result")
                        .param("algorithm", "HeapSort"))
                .andExpect(status().isOk());
    }

    @Test
    void result_volumeCriterion_returns200() throws Exception {
        when(benchmark.sortedSample("QuickSort", "volume", 10))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/etl/sort/result")
                        .param("algorithm", "QuickSort")
                        .param("criterion", "volume")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    @Test
    void result_returnsEmptyListWhenNoData() throws Exception {
        when(benchmark.sortedSample("TimSort", "date", 20)).thenReturn(List.of());

        mockMvc.perform(get("/api/etl/sort/result")
                        .param("algorithm", "TimSort"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private CleanedRecordDto sampleDto() {
        CleanedRecord r = new CleanedRecord();
        r.setSymbol("AAPL");
        r.setDate(LocalDate.of(2023, 1, 10));
        r.setClose(BigDecimal.valueOf(125.0));
        r.setVolume(9000L);
        r.setOpen(BigDecimal.valueOf(125.0));
        r.setHigh(BigDecimal.valueOf(125.0));
        r.setLow(BigDecimal.valueOf(125.0));
        r.setDataQuality(DataQuality.CLEAN);
        r.setTransformedAt(LocalDateTime.now());
        return new CleanedRecordDto(r);
    }
}
