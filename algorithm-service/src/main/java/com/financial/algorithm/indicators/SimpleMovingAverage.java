package com.financial.algorithm.indicators;

import org.springframework.stereotype.Component;

/**
 * Calcula la Media Móvil Simple (SMA) sobre una serie de precios de cierre.
 *
 * <p><b>Fórmula:</b>
 * <pre>
 *   SMA_t(w) = (1/w) · Σ P_i   para i = t-w+1 ... t
 * </pre>
 *
 * <p><b>Complejidad:</b> O(n) temporal (suma deslizante), O(n) espacial.
 *
 * <p><b>Salida:</b> arreglo de igual longitud que la entrada.
 * Los primeros {@code w-1} posiciones contienen {@code Double.NaN} porque no
 * existe historial suficiente para completar la ventana.
 */
@Component
public class SimpleMovingAverage {

    /**
     * Calcula la SMA para cada punto de {@code prices} con ventana {@code w}.
     *
     * @param prices arreglo de precios de cierre en orden cronológico
     * @param w      tamaño de la ventana (debe ser ≥ 1)
     * @return arreglo de SMA; las primeras {@code w-1} posiciones son {@code Double.NaN}
     */
    public double[] compute(double[] prices, int w) {
        double[] result = new double[prices.length];

        if (w > prices.length) {
            for (int i = 0; i < prices.length; i++) {
                result[i] = Double.NaN;
            }
            return result;
        }

        // Inicializar primeras w-1 posiciones como NaN
        for (int i = 0; i < w - 1; i++) {
            result[i] = Double.NaN;
        }

        // Calcular la suma inicial para la primera ventana completa
        double sum = 0.0;
        for (int i = 0; i < w; i++) {
            sum += prices[i];
        }
        result[w - 1] = sum / w;

        // Suma deslizante O(n): añadir nuevo elemento, quitar el más antiguo
        for (int i = w; i < prices.length; i++) {
            sum += prices[i] - prices[i - w];
            result[i] = sum / w;
        }

        return result;
    }
}
