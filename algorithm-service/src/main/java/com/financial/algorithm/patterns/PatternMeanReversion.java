package com.financial.algorithm.patterns;

import com.financial.algorithm.dto.PatternOccurrence;
import com.financial.algorithm.dto.PatternResult;
import com.financial.algorithm.indicators.SimpleMovingAverage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Detecta momentos en que el precio se aleja excesivamente de su media móvil reciente.
 *
 * <p><b>Definición formal:</b>
 * <pre>
 *   El patrón se cumple en t si:
 *   |close_t - SMA_w(t)| > threshold · σ_w(t)
 *
 *   Donde σ_w(t) es la desviación estándar de los últimos w precios hasta t.
 * </pre>
 *
 * <p><b>Complejidad:</b> O(n · w) temporal, O(n) espacial.
 */
@Component
public class PatternMeanReversion {

    private final SimpleMovingAverage sma;

    public PatternMeanReversion(SimpleMovingAverage sma) {
        this.sma = sma;
    }

    /**
     * Detecta señales de reversión a la media con ventana {@code w} y umbral {@code threshold}.
     *
     * @param closePrices precios de cierre en orden cronológico
     * @param w           tamaño de la ventana SMA y de la desviación estándar local
     * @param threshold   número de desviaciones estándar para considerar sobre-extensión
     * @return resultado con conteo, frecuencia relativa y detalle de cada señal
     */
    public PatternResult detect(double[] closePrices, int w, double threshold) {
        double[] smaValues = sma.compute(closePrices, w);

        int count = 0;
        int totalEvaluated = 0;
        List<PatternOccurrence> details = new ArrayList<>();

        for (int t = w - 1; t < closePrices.length; t++) {
            double smaT = smaValues[t];

            // Calcular σ_w(t): desviación estándar de los w precios anteriores
            double sumSq = 0.0;
            for (int i = t - w + 1; i <= t; i++) {
                double diff = closePrices[i] - smaT;
                sumSq += diff * diff;
            }
            double sigma = Math.sqrt(sumSq / w);

            totalEvaluated++;

            if (sigma > 0 && Math.abs(closePrices[t] - smaT) > threshold * sigma) {
                count++;
                PatternOccurrence.Direction dir = closePrices[t] > smaT
                        ? PatternOccurrence.Direction.ABOVE
                        : PatternOccurrence.Direction.BELOW;

                details.add(PatternOccurrence.builder()
                        .startIndex(t)
                        .price(closePrices[t])
                        .sma(smaT)
                        .sigma(sigma)
                        .direction(dir)
                        .build());
            }
        }

        double relativeFrequency = totalEvaluated > 0 ? (double) count / totalEvaluated : 0.0;

        return PatternResult.builder()
                .patternType("MEAN_REVERSION")
                .totalWindows(totalEvaluated)
                .occurrences(count)
                .relativeFrequency(relativeFrequency)
                .details(details)
                .build();
    }
}
