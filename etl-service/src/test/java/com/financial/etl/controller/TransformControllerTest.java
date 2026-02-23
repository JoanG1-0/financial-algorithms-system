package com.financial.etl.controller;

import com.financial.etl.dto.TransformSummary;
import com.financial.etl.entity.DataQuality;
import com.financial.etl.repository.CleanedRecordRepository;
import com.financial.etl.transform.DataTransformService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransformController.class)
class TransformControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataTransformService transformService;

    @MockBean
    private CleanedRecordRepository cleanedRecordRepository;

    @Test
    void postTransform_returns202WithSummary() throws Exception {
        TransformSummary summary = new TransformSummary(
                5, 100, 500L, 480L, 15L, 3L, 2L,
                LocalDateTime.of(2024, 1, 10, 12, 0)
        );
        when(transformService.transformAll()).thenReturn(summary);

        mockMvc.perform(post("/api/etl/transform")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.symbolsProcessed").value(5))
                .andExpect(jsonPath("$.calendarDays").value(100))
                .andExpect(jsonPath("$.totalRecords").value(500))
                .andExpect(jsonPath("$.cleanRecords").value(480))
                .andExpect(jsonPath("$.forwardFilledRecords").value(15))
                .andExpect(jsonPath("$.anomalyCorrectedRecords").value(3))
                .andExpect(jsonPath("$.anomalyFlaggedRecords").value(2));
    }

    @Test
    void getStatus_returns200WithQualityCounts() throws Exception {
        CleanedRecordRepository.QualityCount qc = new CleanedRecordRepository.QualityCount() {
            public DataQuality getQuality() { return DataQuality.CLEAN; }
            public Long getTotal() { return 42L; }
        };

        when(cleanedRecordRepository.countByDataQuality()).thenReturn(List.of(qc));

        mockMvc.perform(get("/api/etl/transform/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CLEAN").value(42))
                .andExpect(jsonPath("$.FORWARD_FILLED").value(0))
                .andExpect(jsonPath("$.ANOMALY_CORRECTED").value(0))
                .andExpect(jsonPath("$.ANOMALY_FLAGGED").value(0));
    }

    @Test
    void getStatus_allZerosWhenNoRecords() throws Exception {
        when(cleanedRecordRepository.countByDataQuality()).thenReturn(List.of());

        mockMvc.perform(get("/api/etl/transform/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CLEAN").value(0))
                .andExpect(jsonPath("$.FORWARD_FILLED").value(0))
                .andExpect(jsonPath("$.ANOMALY_CORRECTED").value(0))
                .andExpect(jsonPath("$.ANOMALY_FLAGGED").value(0));
    }
}
