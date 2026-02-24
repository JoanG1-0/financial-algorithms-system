package com.financial.algorithm.similarity;

import org.springframework.stereotype.Component;

/**
 * Calcula la similitud coseno entre dos series de retornos diarios.
 *
 * <p>Mide la dirección del movimiento entre dos vectores, ignorando la magnitud.
 *
 * <p><b>Fórmula:</b>
 * <pre>
 *   S_C(x, y) = (x · y) / (|x| · |y|)
 *
 *   Donde:
 *     x · y = Σ x_i · y_i        (producto punto)
 *     |x|   = sqrt(Σ x_i²)       (norma euclidiana)
 * </pre>
 *
 * <p><b>Complejidad:</b> O(n) temporal, O(1) espacial.
 *
 * <p><b>Salida:</b> {@code double} en [-1, 1].
 * Retorna {@code Double.NaN} si alguno de los vectores es el vector cero.
 */
@Component
public class CosineSimilarity {

    /**
     * Calcula la similitud coseno entre {@code x} e {@code y}.
     *
     * @param x primera serie de retornos diarios
     * @param y segunda serie de retornos diarios
     * @return similitud en [-1, 1], o {@code Double.NaN} si algún vector es cero
     * @throws IllegalArgumentException si las series tienen longitudes distintas
     */
    public double compute(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException(
                    "Las series deben tener la misma longitud: " + x.length + " vs " + y.length);
        }
        if (x.length == 0) {
            return Double.NaN;
        }

        double dotProduct = 0.0;
        double normX = 0.0;
        double normY = 0.0;
        for (int i = 0; i < x.length; i++) {
            dotProduct += x[i] * y[i];
            normX += x[i] * x[i];
            normY += y[i] * y[i];
        }

        double denominator = Math.sqrt(normX) * Math.sqrt(normY);
        if (denominator == 0.0) {
            return Double.NaN;
        }
        return dotProduct / denominator;
    }
}
