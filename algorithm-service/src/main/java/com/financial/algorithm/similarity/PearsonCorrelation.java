package com.financial.algorithm.similarity;

import org.springframework.stereotype.Component;

/**
 * Calcula la correlación de Pearson entre dos series de retornos diarios.
 *
 * <p><b>Fórmula:</b>
 * <pre>
 *   r = Σ[(x_i - x̄)(y_i - ȳ)] / sqrt( Σ(x_i - x̄)² · Σ(y_i - ȳ)² )
 * </pre>
 *
 * <p><b>Complejidad:</b> O(n) temporal, O(1) espacial.
 *
 * <p><b>Salida:</b> {@code double} en [-1, 1].
 * Retorna {@code Double.NaN} si alguna de las series es constante (σ = 0).
 */
@Component
public class PearsonCorrelation {

    /**
     * Calcula la correlación de Pearson entre {@code x} e {@code y}.
     *
     * @param x primera serie de retornos diarios
     * @param y segunda serie de retornos diarios
     * @return correlación en [-1, 1], o {@code Double.NaN} si σ = 0 en alguna serie
     * @throws IllegalArgumentException si las series tienen longitudes distintas o están vacías
     */
    public double compute(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException(
                    "Las series deben tener la misma longitud: " + x.length + " vs " + y.length);
        }
        if (x.length == 0) {
            throw new IllegalArgumentException("Las series no pueden estar vacías");
        }

        double sumX = 0.0;
        double sumY = 0.0;
        
        for (int i = 0; i < x.length; i++) {
            sumX += x[i];
            sumY += y[i];
        }
        double meanX = sumX / x.length;
        double meanY = sumY / y.length;

        double numerator = 0.0;
        double denomX = 0.0;
        double denomY = 0.0;
        for (int i = 0; i < x.length; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            numerator += dx * dy;
            denomX += dx * dx;
            denomY += dy * dy;
        }

        double denominator = Math.sqrt(denomX * denomY);
        if (denominator == 0.0) {
            return Double.NaN;
        }
        return numerator / denominator;
    }
}
