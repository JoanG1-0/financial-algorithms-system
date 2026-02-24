package com.financial.report.correlation;

import com.financial.report.dto.SimilarityResponse;
import com.financial.report.entity.CorrelationEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationMatrixBuilderTest {

    private CorrelationMatrixBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new CorrelationMatrixBuilder();
    }

    /**
     * Con 3 activos (A, B, C) hay 3 pares off-diagonal + 3 entradas de diagonal = 9.
     */
    @Test
    void build_threeAssets_returnsNineEntries() {
        List<SimilarityResponse> similarities = List.of(
                pair("AAA", "BBB", 0.75),
                pair("AAA", "CCC", 0.50),
                pair("BBB", "CCC", 0.60)
        );

        List<CorrelationEntry> entries = builder.build(similarities);

        assertEquals(9, entries.size());
    }

    @Test
    void build_diagonalEntriesHaveCorrelationOne() {
        List<SimilarityResponse> similarities = List.of(
                pair("AAA", "BBB", 0.75),
                pair("AAA", "CCC", 0.50),
                pair("BBB", "CCC", 0.60)
        );

        List<CorrelationEntry> entries = builder.build(similarities);

        long diagCount = entries.stream()
                .filter(e -> e.getAssetA().equals(e.getAssetB()))
                .filter(e -> e.getCorrelation() == 1.0)
                .count();
        assertEquals(3, diagCount);
    }

    @Test
    void build_pearsonValuesCorrectlyExtracted() {
        List<SimilarityResponse> similarities = List.of(
                pair("AAA", "BBB", 0.75),
                pair("AAA", "CCC", 0.50)
        );

        List<CorrelationEntry> entries = builder.build(similarities);

        CorrelationEntry aaBB = entries.stream()
                .filter(e -> e.getAssetA().equals("AAA") && e.getAssetB().equals("BBB"))
                .findFirst()
                .orElseThrow();
        assertEquals(0.75, aaBB.getCorrelation(), 1e-9);

        CorrelationEntry aaCC = entries.stream()
                .filter(e -> e.getAssetA().equals("AAA") && e.getAssetB().equals("CCC"))
                .findFirst()
                .orElseThrow();
        assertEquals(0.50, aaCC.getCorrelation(), 1e-9);
    }

    @Test
    void build_emptyInput_returnsEmptyList() {
        List<CorrelationEntry> entries = builder.build(List.of());
        assertTrue(entries.isEmpty());
    }

    // Helper
    private SimilarityResponse pair(String tickerA, String tickerB, double pearson) {
        SimilarityResponse s = new SimilarityResponse();
        s.setTickerA(tickerA);
        s.setTickerB(tickerB);
        s.setPearson(pearson);
        return s;
    }
}
