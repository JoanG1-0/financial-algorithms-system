package com.financial.report.pdf;

import com.financial.report.dto.PatternResponse;
import com.financial.report.dto.RiskResponse;
import com.financial.report.dto.SimilarityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfReportGeneratorTest {

    private PdfReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PdfReportGenerator();
    }

    @Test
    void generate_withMinimalData_returnsNonEmptyByteArray() {
        RiskResponse risk = new RiskResponse();
        risk.setTicker("AAPL");
        risk.setAnnualizedVolatility(0.25);
        risk.setCategory("MODERATE");
        risk.setComputedAt(LocalDateTime.now());

        SimilarityResponse similarity = new SimilarityResponse();
        similarity.setTickerA("AAPL");
        similarity.setTickerB("MSFT");
        similarity.setEuclidean(0.12);
        similarity.setPearson(0.85);
        similarity.setDtw(3.5);
        similarity.setCosine(0.92);
        similarity.setComputedAt(LocalDateTime.now());

        PatternResponse pattern = new PatternResponse();
        pattern.setSymbol("AAPL");
        pattern.setPatternType("CONSECUTIVE_UP");
        pattern.setOccurrences(5);
        pattern.setRelativeFrequency(0.025);
        pattern.setComputedAt(LocalDateTime.now());

        byte[] pdf = generator.generate(List.of(risk), List.of(similarity), List.of(pattern));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generate_resultStartsWithPdfMagicNumber() {
        RiskResponse risk = new RiskResponse();
        risk.setTicker("VOO");
        risk.setAnnualizedVolatility(0.15);
        risk.setCategory("CONSERVATIVE");
        risk.setComputedAt(LocalDateTime.now());

        SimilarityResponse similarity = new SimilarityResponse();
        similarity.setTickerA("VOO");
        similarity.setTickerB("SPY");
        similarity.setPearson(0.99);
        similarity.setComputedAt(LocalDateTime.now());

        PatternResponse pattern = new PatternResponse();
        pattern.setSymbol("VOO");
        pattern.setPatternType("MEAN_REVERSION");
        pattern.setOccurrences(3);
        pattern.setRelativeFrequency(0.015);
        pattern.setComputedAt(LocalDateTime.now());

        byte[] pdf = generator.generate(List.of(risk), List.of(similarity), List.of(pattern));

        // PDF files always start with the magic number "%PDF"
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }
}
