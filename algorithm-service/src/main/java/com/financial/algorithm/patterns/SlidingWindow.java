package com.financial.algorithm.patterns;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extrae todas las sub-ventanas de tamaño {@code k} de una serie temporal.
 *
 * <p>Recorre la serie de izquierda a derecha produciendo {@code (n - k + 1)} ventanas.
 * Es la utilidad base reutilizada por los detectores de patrones.
 *
 * <p><b>Complejidad:</b> O(n) temporal, O(k) espacial por ventana.
 */
@Component
public class SlidingWindow {

    /**
     * Representa una sub-ventana extraída de la serie original.
     *
     * @param startIndex índice de inicio en la serie original
     * @param values     valores de la ventana (copia)
     */
    public record WindowSlice(int startIndex, double[] values) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WindowSlice other)) return false;
            return startIndex == other.startIndex && Arrays.equals(values, other.values);
        }

        @Override
        public int hashCode() {
            return 31 * Integer.hashCode(startIndex) + Arrays.hashCode(values);
        }

        @Override
        public String toString() {
            return "WindowSlice[startIndex=" + startIndex + ", values=" + Arrays.toString(values) + "]";
        }
    }

    /**
     * Extrae todas las sub-ventanas de tamaño {@code k} de {@code series}.
     *
     * @param series serie de valores en orden cronológico
     * @param k      tamaño de la ventana
     * @return lista de {@code WindowSlice}; vacía si {@code k > series.length}
     */
    public List<WindowSlice> extract(double[] series, int k) {
        List<WindowSlice> windows = new ArrayList<>();
        if (k > series.length || k <= 0) {
            return windows;
        }
        for (int i = 0; i <= series.length - k; i++) {
            windows.add(new WindowSlice(i, Arrays.copyOfRange(series, i, i + k)));
        }
        return windows;
    }
}
