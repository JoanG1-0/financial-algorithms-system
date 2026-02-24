package com.financial.algorithm.patterns;

import com.financial.algorithm.dto.PatternResult;
import com.financial.algorithm.indicators.SimpleMovingAverage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatternMeanReversionTest {

    private PatternMeanReversion detector;

    @BeforeEach
    void setUp() {
        detector = new PatternMeanReversion(new SimpleMovingAverage());
    }

    @Test
    void detect_stablePrices_noSignals() {
        // Precios constantes → σ = 0 → condición sigma > 0 no se cumple
        double[] prices = {100.0, 100.0, 100.0, 100.0, 100.0, 100.0};
        PatternResult result = detector.detect(prices, 3, 2.0);

        assertEquals(0, result.getOccurrences());
    }

    @Test
    void detect_spikePrice_detectsSignal() {
        // Spike de 200 en ventana de 5 → el outlier es exactamente sqrt(4)=2σ de la media.
        // Con threshold=1.5 (< 2.0), 2σ > 1.5σ → señal detectada.
        double[] prices = {100.0, 100.0, 100.0, 100.0, 100.0, 200.0};
        PatternResult result = detector.detect(prices, 5, 1.5);

        assertTrue(result.getOccurrences() >= 1);
        assertFalse(result.getDetails().isEmpty());
    }

    @Test
    void detect_spikeAboveMean_directionAbove() {
        // Spike por encima: threshold=1.5 garantiza detección (outlier = 2σ > 1.5σ)
        double[] prices = {100.0, 100.0, 100.0, 100.0, 100.0, 300.0};
        PatternResult result = detector.detect(prices, 5, 1.5);

        boolean hasAbove = result.getDetails().stream()
                .anyMatch(o -> o.getDirection() == com.financial.algorithm.dto.PatternOccurrence.Direction.ABOVE);
        assertTrue(hasAbove);
    }
}
