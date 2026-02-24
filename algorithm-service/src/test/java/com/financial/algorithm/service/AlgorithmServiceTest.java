package com.financial.algorithm.service;

import com.financial.algorithm.client.EtlServiceClient;
import com.financial.algorithm.entity.PatternRecord;
import com.financial.algorithm.entity.RiskRecord;
import com.financial.algorithm.entity.SimilarityRecord;
import com.financial.algorithm.entity.SmaRecord;
import com.financial.algorithm.indicators.SimpleMovingAverage;
import com.financial.algorithm.patterns.PatternConsecutiveUp;
import com.financial.algorithm.patterns.PatternMeanReversion;
import com.financial.algorithm.repository.PatternRepository;
import com.financial.algorithm.repository.RiskRepository;
import com.financial.algorithm.repository.SimilarityRepository;
import com.financial.algorithm.repository.SmaRepository;
import com.financial.algorithm.risk.RiskClassifier;
import com.financial.algorithm.risk.VolatilityCalculator;
import com.financial.algorithm.similarity.CosineSimilarity;
import com.financial.algorithm.similarity.DynamicTimeWarping;
import com.financial.algorithm.similarity.EuclideanDistance;
import com.financial.algorithm.similarity.PearsonCorrelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlgorithmServiceTest {

    @Mock private EtlServiceClient etlServiceClient;
    @Mock private SimilarityRepository similarityRepository;
    @Mock private RiskRepository riskRepository;
    @Mock private PatternRepository patternRepository;
    @Mock private SmaRepository smaRepository;

    private AlgorithmService service;

    @BeforeEach
    void setUp() {
        // Construir las dependencias de lógica pura sin mocks
        EuclideanDistance euclidean = new EuclideanDistance();
        PearsonCorrelation pearson = new PearsonCorrelation();
        DynamicTimeWarping dtw = new DynamicTimeWarping();
        CosineSimilarity cosine = new CosineSimilarity();
        VolatilityCalculator volCalc = new VolatilityCalculator();
        RiskClassifier riskClassifier = new RiskClassifier(volCalc);
        SimpleMovingAverage sma = new SimpleMovingAverage();
        PatternConsecutiveUp consecutiveUp = new PatternConsecutiveUp(
                new com.financial.algorithm.patterns.SlidingWindow());
        PatternMeanReversion meanReversion = new PatternMeanReversion(sma);

        service = new AlgorithmService(
                etlServiceClient,
                euclidean, pearson, dtw, cosine,
                riskClassifier,
                consecutiveUp, meanReversion, sma,
                similarityRepository, riskRepository, patternRepository, smaRepository
        );
    }

    @Test
    void runFullAnalysis_persistsAllCategories() {
        // Dos activos con series suficientes
        double[] pricesA = buildPrices(50, 100.0, 0.01);
        double[] pricesB = buildPrices(50, 200.0, -0.01);
        Map<String, double[]> portfolio = Map.of("AAA", pricesA, "BBB", pricesB);

        when(etlServiceClient.fetchCleanedPrices()).thenReturn(portfolio);
        when(similarityRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(riskRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(patternRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(smaRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        AlgorithmService.AnalysisSummary summary = service.runFullAnalysis();

        // 2 activos → 1 par de similitud
        assertEquals(1, summary.similarityPairs());
        // 2 perfiles de riesgo
        assertEquals(2, summary.riskProfiles());
        // 2 activos × 2 patrones = 4 registros
        assertEquals(4, summary.patternRecords());
        // 2 registros SMA
        assertEquals(2, summary.smaRecords());

        verify(similarityRepository).saveAll(any());
        verify(riskRepository).saveAll(any());
        verify(patternRepository).saveAll(any());
        verify(smaRepository).saveAll(any());
    }

    @Test
    void getPatternsBySymbol_delegatesToRepository() {
        PatternRecord rec = new PatternRecord();
        rec.setSymbol("AAA");
        when(patternRepository.findBySymbol("AAA")).thenReturn(List.of(rec));

        List<PatternRecord> result = service.getPatternsBySymbol("AAA");

        assertEquals(1, result.size());
        assertEquals("AAA", result.get(0).getSymbol());
    }

    @Test
    void getSma_returnsNullWhenNotFound() {
        when(smaRepository.findTopBySymbolAndWindowOrderByComputedAtDesc("UNKNOWN", 20))
                .thenReturn(Optional.empty());

        assertNull(service.getSma("UNKNOWN", 20));
    }

    // Genera precios con tendencia lineal
    private double[] buildPrices(int n, double start, double dailyChange) {
        double[] prices = new double[n];
        prices[0] = start;
        for (int i = 1; i < n; i++) {
            prices[i] = prices[i - 1] * (1 + dailyChange);
        }
        return prices;
    }
}
