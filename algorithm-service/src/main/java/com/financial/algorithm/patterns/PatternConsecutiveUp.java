package com.financial.algorithm.patterns;

import com.financial.algorithm.dto.PatternOccurrence;
import com.financial.algorithm.dto.PatternResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Detecta secuencias de {@code k} días consecutivos al alza en una serie de precios.
 *
 * <p><b>Definición formal:</b>
 * <pre>
 *   El patrón se cumple si: ∀ i ∈ [1, k] : retorno_i > 0
 *   Donde retorno_i = (close_i - close_(i-1)) / close_(i-1)
 * </pre>
 *
 * <p><b>Complejidad:</b> O(n) temporal.
 */
@Component
public class PatternConsecutiveUp {

    private final SlidingWindow slidingWindow;

    public PatternConsecutiveUp(SlidingWindow slidingWindow) {
        this.slidingWindow = slidingWindow;
    }

    /**
     * Detecta ventanas de {@code k} días con todos los retornos positivos.
     *
     * @param closePrices precios de cierre en orden cronológico
     * @param k           tamaño de la ventana (días consecutivos al alza)
     * @return resultado con conteo, frecuencia relativa e índices de inicio
     */
    public PatternResult detect(double[] closePrices, int k) {
        // Calcular retornos diarios: necesitamos k+1 precios para k retornos
        List<SlidingWindow.WindowSlice> windows = slidingWindow.extract(closePrices, k + 1);

        int count = 0;
        List<PatternOccurrence> details = new ArrayList<>();

        for (SlidingWindow.WindowSlice window : windows) {
            double[] prices = window.values();
            boolean allUp = true;
            for (int i = 1; i < prices.length; i++) {
                double ret = (prices[i] - prices[i - 1]) / prices[i - 1];
                if (ret <= 0) {
                    allUp = false;
                    break;
                }
            }
            if (allUp) {
                count++;
                details.add(PatternOccurrence.builder()
                        .startIndex(window.startIndex())
                        .build());
            }
        }

        int totalWindows = windows.size();
        double relativeFrequency = totalWindows > 0 ? (double) count / totalWindows : 0.0;

        return PatternResult.builder()
                .patternType("CONSECUTIVE_UP")
                .totalWindows(totalWindows)
                .occurrences(count)
                .relativeFrequency(relativeFrequency)
                .details(details)
                .build();
    }
}
