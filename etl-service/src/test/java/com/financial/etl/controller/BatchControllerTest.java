package com.financial.etl.controller;

import com.financial.etl.entity.BatchDownloadLog;
import com.financial.etl.entity.BatchStatus;
import com.financial.etl.service.BatchDownloadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchController.class)
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BatchDownloadService batchDownloadService;

    @Test
    void getStatus_returns200WithLog_whenBatchExistsToday() throws Exception {
        BatchDownloadLog log = new BatchDownloadLog();
        log.setId(1L);
        log.setDownloadDate(LocalDate.now());
        log.setStatus(BatchStatus.COMPLETED);
        log.setTotalSymbols(20);
        log.setDownloadedSymbols(20);
        log.setFailedSymbols(0);
        log.setStartedAt(LocalDateTime.now().minusMinutes(10));
        log.setCompletedAt(LocalDateTime.now());

        when(batchDownloadService.getLastBatchStatus()).thenReturn(Optional.of(log));

        mockMvc.perform(get("/api/etl/batch/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalSymbols").value(20))
                .andExpect(jsonPath("$.downloadedSymbols").value(20));
    }

    @Test
    void getStatus_returns404_whenNoBatchExists() throws Exception {
        when(batchDownloadService.getLastBatchStatus()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/etl/batch/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void triggerBatch_returns202Accepted() throws Exception {
        doNothing().when(batchDownloadService).downloadAllSymbols();

        mockMvc.perform(post("/api/etl/batch/trigger")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(batchDownloadService).downloadAllSymbols();
    }
}
