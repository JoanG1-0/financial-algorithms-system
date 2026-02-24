package com.financial.algorithm.patterns;

import com.financial.algorithm.dto.PatternResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatternConsecutiveUpTest {

    private PatternConsecutiveUp detector;

    @BeforeEach
    void setUp() {
        detector = new PatternConsecutiveUp(new SlidingWindow());
    }

    @Test
    void detect_fiveConsecutiveDaysUp_detectsOneOccurrence() {
        // 6 precios → 5 retornos todos positivos
        double[] prices = {10.0, 11.0, 12.0, 13.0, 14.0, 15.0};
        PatternResult result = detector.detect(prices, 5);

        assertEquals(1, result.getOccurrences());
        assertEquals(1.0, result.getRelativeFrequency(), 1e-9);
    }

    @Test
    void detect_mixedDays_detectsOnlyConsecutiveWindows() {
        // Precios: alza, alza, baja, alza, alza → con k=2 detectar ventanas de 2 alzas
        double[] prices = {10.0, 11.0, 12.0, 11.0, 12.0, 13.0};
        PatternResult result = detector.detect(prices, 2);

        // Ventanas (de 3 precios): [10,11,12], [11,12,11], [12,11,12], [11,12,13]
        // Retornos por ventana: [+,+], [+,-], [-,+], [+,+]
        // Matches: ventana 0 y ventana 3
        assertEquals(2, result.getOccurrences());
    }

    @Test
    void detect_allDown_zeroOccurrences() {
        double[] prices = {15.0, 14.0, 13.0, 12.0, 11.0};
        PatternResult result = detector.detect(prices, 3);

        assertEquals(0, result.getOccurrences());
        assertEquals(0.0, result.getRelativeFrequency(), 1e-9);
    }
}
