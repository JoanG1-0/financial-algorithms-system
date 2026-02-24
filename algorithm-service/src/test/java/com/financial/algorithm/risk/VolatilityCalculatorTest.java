package com.financial.algorithm.risk;

import com.financial.algorithm.dto.VolatilityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VolatilityCalculatorTest {

    private VolatilityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new VolatilityCalculator();
    }

    @Test
    void compute_constantPrices_returnsZeroSigma() {
        double[] prices = {100.0, 100.0, 100.0, 100.0, 100.0};
        VolatilityResult result = calculator.compute(prices);

        assertEquals(0.0, result.getDailySigma(), 1e-9);
        assertEquals(0.0, result.getAnnualizedSigma(), 1e-9);
    }

    @Test
    void compute_twoPrices_returnsCorrectLogReturn() {
        // ln(e) = 1 → un solo retorno
        double[] prices = {1.0, Math.E};
        VolatilityResult result = calculator.compute(prices);

        assertEquals(1, result.getLogReturns().length);
        assertEquals(1.0, result.getLogReturns()[0], 1e-9);
        // Con un solo retorno, media = retorno, varianza = 0 → sigma = 0
        assertEquals(0.0, result.getDailySigma(), 1e-9);
    }

    @Test
    void compute_annualizedSigma_isScaledBySqrt252() {
        // Precios con variación conocida
        double[] prices = {100.0, 110.0, 100.0, 110.0, 100.0};
        VolatilityResult result = calculator.compute(prices);

        assertEquals(result.getDailySigma() * Math.sqrt(252),
                     result.getAnnualizedSigma(), 1e-9);
    }

    @Test
    void compute_lessThanTwoPrices_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.compute(new double[]{100.0}));
    }
}
