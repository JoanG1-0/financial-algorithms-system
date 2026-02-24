package com.financial.algorithm.similarity;

import org.springframework.stereotype.Component;

/**
 * Calcula la distancia DTW (Dynamic Time Warping) entre dos series temporales.
 *
 * <p>DTW permite comparar series con desfases temporales buscando la alineación
 * óptima entre ellas, a diferencia de la distancia euclidiana punto a punto.
 *
 * <p><b>Fórmula:</b>
 * <pre>
 *   M[i][j] = |a_i - b_j| + min(M[i-1][j], M[i][j-1], M[i-1][j-1])
 *   Retorna: M[n-1][m-1]
 * </pre>
 *
 * <p><b>Complejidad:</b> O(n × m) temporal y espacial.
 *
 * <p><b>Salida:</b> {@code double} ≥ 0. Cuanto menor, más similares son las series.
 */
@Component
public class DynamicTimeWarping {

    /**
     * Calcula la distancia DTW entre {@code a} y {@code b}.
     *
     * @param a primera serie de retornos diarios
     * @param b segunda serie de retornos diarios (puede tener longitud distinta)
     * @return costo acumulado de alineación óptima ≥ 0
     */
    public double compute(double[] a, double[] b) {
        int n = a.length;
        int m = b.length;

        if (n == 0 || m == 0) {
            return 0.0;
        }

        double[][] dtw = new double[n][m];

        // Inicializar todas las celdas con infinito
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                dtw[i][j] = Double.MAX_VALUE;
            }
        }

        dtw[0][0] = Math.abs(a[0] - b[0]);

        // Primera columna
        for (int i = 1; i < n; i++) {
            dtw[i][0] = Math.abs(a[i] - b[0]) + dtw[i - 1][0];
        }

        // Primera fila
        for (int j = 1; j < m; j++) {
            dtw[0][j] = Math.abs(a[0] - b[j]) + dtw[0][j - 1];
        }

        // Resto de la matriz
        for (int i = 1; i < n; i++) {
            for (int j = 1; j < m; j++) {
                double cost = Math.abs(a[i] - b[j]);
                double minPrev = Math.min(dtw[i - 1][j],
                                 Math.min(dtw[i][j - 1], dtw[i - 1][j - 1]));
                dtw[i][j] = cost + minPrev;
            }
        }

        return dtw[n - 1][m - 1];
    }
}
