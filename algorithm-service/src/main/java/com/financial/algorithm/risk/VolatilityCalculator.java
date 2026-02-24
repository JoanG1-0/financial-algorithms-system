package com.financial.algorithm.risk;

import com.financial.algorithm.dto.VolatilityResult;
import org.springframework.stereotype.Component;

/**
 * Calcula la volatilidad histórica de un activo a partir de sus precios de cierre.
 *
 * <p><b>Fórmulas:</b>
 * <pre>
 *   Retorno logarítmico:  r_i = ln(close_i / close_(i-1))
 *   Desviación estándar:  σ = sqrt( (1/n) · Σ(r_i - r̄)² )
 *   Volatilidad anual:    σ_anual = σ · sqrt(252)
 * </pre>
 *
 * <p>Se usa 252 días hábiles bursátiles como factor de anualización estándar.
 *
 * <p><b>Complejidad:</b> O(n) temporal, O(n) espacial (almacena retornos).
 *
 * <p><b>Salida:</b> {@link VolatilityResult} con retornos, σ diaria y σ anualizada.
 */
@Component
public class VolatilityCalculator {

    private static final double ANNUALIZATION_FACTOR = Math.sqrt(252);

    /**
     * Calcula la volatilidad del activo representado por {@code closePrices}.
     *
     * @param closePrices precios de cierre en orden cronológico (mínimo 2 elementos)
     * @return resultado con retornos logarítmicos, σ diaria y σ anualizada
     * @throws IllegalArgumentException si hay menos de 2 precios
     */
    public VolatilityResult compute(double[] closePrices) {
        if (closePrices.length < 2) {
            throw new IllegalArgumentException(
                    "Se necesitan al menos 2 precios para calcular volatilidad");
        }

        int n = closePrices.length - 1;
        double[] logReturns = new double[n];

        // Calcular retornos logarítmicos y su media
        double sumReturns = 0.0;
        for (int i = 0; i < n; i++) {
            logReturns[i] = Math.log(closePrices[i + 1] / closePrices[i]);
            sumReturns += logReturns[i];
        }
        double meanReturn = sumReturns / n;

        // Calcular varianza (desviación estándar poblacional)
        double sumSq = 0.0;
        for (int i = 0; i < n; i++) {
            double diff = logReturns[i] - meanReturn;
            sumSq += diff * diff;
        }
        double dailySigma = Math.sqrt(sumSq / n);
        double annualizedSigma = dailySigma * ANNUALIZATION_FACTOR;

        return VolatilityResult.builder()
                .logReturns(logReturns)
                .dailySigma(dailySigma)
                .annualizedSigma(annualizedSigma)
                .build();
    }
}
