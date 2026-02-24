package com.financial.algorithm.risk;

import com.financial.algorithm.dto.RiskProfile;
import com.financial.algorithm.dto.RiskProfile.RiskCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RiskClassifierTest {

    private RiskClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new RiskClassifier(new VolatilityCalculator());
    }

    @Test
    void classify_threeAssets_returnsAscendingOrder() {
        // CONSERVATIVE: precios constantes → σ=0
        double[] conservative = new double[100];
        for (int i = 0; i < 100; i++) conservative[i] = 100.0;

        // AGGRESSIVE: variación grande alternando 50/150
        double[] aggressive = new double[100];
        for (int i = 0; i < 100; i++) aggressive[i] = (i % 2 == 0) ? 50.0 : 150.0;

        // MODERATE: variación moderada alternando 95/105
        double[] moderate = new double[100];
        for (int i = 0; i < 100; i++) moderate[i] = (i % 2 == 0) ? 95.0 : 105.0;

        Map<String, double[]> portfolio = new LinkedHashMap<>();
        portfolio.put("AGGRESSIVE_ASSET", aggressive);
        portfolio.put("CONSERVATIVE_ASSET", conservative);
        portfolio.put("MODERATE_ASSET", moderate);

        List<RiskProfile> result = classifier.classify(portfolio);

        assertEquals(3, result.size());

        // Debe estar ordenado ascendentemente por volatilidad
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).getAnnualizedVolatility()
                    <= result.get(i + 1).getAnnualizedVolatility());
        }

        // El primero debe ser CONSERVATIVE (σ=0)
        assertEquals(RiskCategory.CONSERVATIVE, result.get(0).getCategory());
    }

    @Test
    void classify_constantPrices_isConservative() {
        double[] prices = new double[50];
        for (int i = 0; i < 50; i++) prices[i] = 100.0;

        Map<String, double[]> portfolio = Map.of("STABLE", prices);
        List<RiskProfile> result = classifier.classify(portfolio);

        assertEquals(RiskCategory.CONSERVATIVE, result.get(0).getCategory());
        assertEquals(0.0, result.get(0).getAnnualizedVolatility(), 1e-9);
    }

    @Test
    void classify_highVariation_isAggressive() {
        // Alternando 1 y 1000 → retornos enormes → σ_anual > 0.30
        double[] prices = new double[50];
        for (int i = 0; i < 50; i++) prices[i] = (i % 2 == 0) ? 1.0 : 1000.0;

        Map<String, double[]> portfolio = Map.of("VOLATILE", prices);
        List<RiskProfile> result = classifier.classify(portfolio);

        assertEquals(RiskCategory.AGGRESSIVE, result.get(0).getCategory());
    }
}
