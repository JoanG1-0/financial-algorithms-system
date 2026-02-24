package com.financial.report.service;

import com.financial.report.client.AlgorithmServiceClient;
import com.financial.report.dto.PatternResponse;
import com.financial.report.dto.RiskResponse;
import com.financial.report.dto.SimilarityResponse;
import com.financial.report.pdf.PdfReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfReportServiceTest {

    @Mock
    private AlgorithmServiceClient client;

    @Mock
    private PdfReportGenerator pdfReportGenerator;

    private PdfReportService pdfReportService;

    @BeforeEach
    void setUp() {
        pdfReportService = new PdfReportService(client, pdfReportGenerator);
    }

    @Test
    void generatePdf_invokesAllClientMethodsAndReturnsGeneratorResult() {
        List<RiskResponse> risks = List.of(new RiskResponse());
        List<SimilarityResponse> sims = List.of(new SimilarityResponse());
        List<PatternResponse> patterns = List.of(new PatternResponse());
        byte[] expectedPdf = new byte[]{0x25, 0x50, 0x44, 0x46};

        when(client.fetchRiskProfiles()).thenReturn(risks);
        when(client.fetchSimilarities()).thenReturn(sims);
        when(client.fetchAllPatterns()).thenReturn(patterns);
        when(pdfReportGenerator.generate(risks, sims, patterns)).thenReturn(expectedPdf);

        byte[] result = pdfReportService.generatePdf();

        verify(client).fetchRiskProfiles();
        verify(client).fetchSimilarities();
        verify(client).fetchAllPatterns();
        verify(pdfReportGenerator).generate(risks, sims, patterns);
        assertArrayEquals(expectedPdf, result);
    }
}
