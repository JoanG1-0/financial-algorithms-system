package com.financial.algorithm.similarity;

import org.springframework.stereotype.Component;

/**
 * Calcula la distancia euclidiana entre dos series de retornos diarios.
 *
 * <p><b>Fórmula:</b>
 * <pre>
 *   d(a, b) = sqrt( Σ (a_i - b_i)² )   para i = 0 ... n-1
 * </pre>
 *
 * <p><b>Complejidad:</b> O(n) temporal, O(1) espacial.
 *
 * <p><b>Salida:</b> {@code double} ≥ 0.  El valor 0 indica series idénticas.
 */
@Component
public class EuclideanDistance {

    /**
     * Calcula la distancia euclidiana entre {@code a} y {@code b}.
     *
     * @param a primera serie de retornos diarios
     * @param b segunda serie de retornos diarios
     * @return distancia ≥ 0
     * @throws IllegalArgumentException si las series tienen longitudes distintas
     */
    public double compute(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Las series deben tener la misma longitud: " + a.length + " vs " + b.length);
        }
        if (a.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
