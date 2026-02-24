package com.financial.report.controller;

import com.financial.report.entity.CorrelationEntry;
import com.financial.report.service.PdfReportService;
import com.financial.report.service.ReportService;
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

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private PdfReportService pdfReportService;

    @Test
    void generate_returns202WithSummary() throws Exception {
        ReportService.CorrelationSummary summary = new ReportService.CorrelationSummary(12);
        when(reportService.generateCorrelationMatrix()).thenReturn(summary);

        mockMvc.perform(post("/api/v1/reports/correlation-matrix/generate"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.entriesCount").value(12));
    }

    @Test
    void correlationMatrix_returns200WithList() throws Exception {
        CorrelationEntry entry = new CorrelationEntry();
        entry.setAssetA("AAPL");
        entry.setAssetB("MSFT");
        entry.setCorrelation(0.85);
        entry.setComputedAt(LocalDateTime.now());
        when(reportService.getCorrelationMatrix()).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/reports/correlation-matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assetA").value("AAPL"))
                .andExpect(jsonPath("$[0].assetB").value("MSFT"))
                .andExpect(jsonPath("$[0].correlation").value(0.85));
    }

    @Test
    void exportPdf_returns200WithPdfContentType() throws Exception {
        byte[] fakePdf = new byte[]{0x25, 0x50, 0x44, 0x46};
        when(pdfReportService.generatePdf()).thenReturn(fakePdf);

        mockMvc.perform(get("/api/v1/reports/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
