package com.financial.etl.transform.cleaner;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detecta retornos extremos de precios mediante análisis Z-score sobre retornos logarítmicos.
 *
 * Algoritmo:
 * 1. Calcula retornos logarítmicos solo sobre registros consecutivos NO rellenos (non-forward-filled)
 *    r_t = ln(close_t / close_{t-1})
 * 2. Calcula μ y σ sobre esos retornos
 * 3. Para cada registro (incluidos FORWARD_FILLED), calcula el z-score usando el último cierre real conocido
 * 4. Si |z| > Z_THRESHOLD y el registro es CLEAN o ANOMALY_CORRECTED → ANOMALY_FLAGGED
 *
 * Complejidad: O(D) por activo, 2 pasadas.
 */
@Service
public class AnomalyDetector {

    static final double Z_THRESHOLD = 3.0;

    public static final String EXTREME_RETURN = "EXTREME_RETURN";

    /**
     * Marca como ANOMALY_FLAGGED los registros con retornos logarítmicos extremos.
     * NO modifica registros ya marcados por otras razones.
     *
     * @param records registros alineados de un único activo (salida de MissingValueCleaner)
     * @return la misma lista, modificada en el lugar donde se detectaron anomalías
     */
    public List<CleanedRecord> detectExtremeReturns(List<CleanedRecord> records) {
        if (records.size() < 2) {
            return records;
        }

        double[] stats = collectReturnStats(records);
        int count = (int) stats[2];
        if (count < 2) {
            return records;
        }

        double mean = stats[0] / count;
        double variance = (stats[1] / count) - (mean * mean);
        double stdDev = Math.sqrt(Math.max(variance, 0.0));

        if (stdDev == 0.0) {
            return records;
        }

        flagExtremeReturns(records, mean, stdDev);
        return records;
    }

    // Pasada 1: recolecta retornos logarítmicos de pares consecutivos no rellenos
    // Devuelve [sumR, sumR2, count]
    private double[] collectReturnStats(List<CleanedRecord> records) {
        double sumR = 0.0;
        double sumR2 = 0.0;
        int count = 0;
        BigDecimal prevClose = null;
        boolean prevWasReal = false;

        for (CleanedRecord cr : records) {
            boolean isReal = cr.getDataQuality() != DataQuality.FORWARD_FILLED;
            if (prevClose != null && prevWasReal && isReal && cr.getClose() != null) {
                double logReturn = computeLogReturn(prevClose, cr.getClose());
                if (Double.isFinite(logReturn)) {
                    sumR += logReturn;
                    sumR2 += logReturn * logReturn;
                    count++;
                }
            }
            if (isReal && cr.getClose() != null) {
                prevClose = cr.getClose();
                prevWasReal = true;
            } else if (!isReal) {
                prevWasReal = false;
            }
        }

        return new double[]{sumR, sumR2, count};
    }

    // Pasada 2: marca registros cuyo z-score supera el umbral
    private void flagExtremeReturns(List<CleanedRecord> records, double mean, double stdDev) {
        BigDecimal lastRealClose = null;
        for (CleanedRecord cr : records) {
            boolean isReal = cr.getDataQuality() != DataQuality.FORWARD_FILLED;
            if (lastRealClose != null && cr.getClose() != null) {
                applyZScoreFlag(cr, lastRealClose, mean, stdDev);
            }
            if (isReal && cr.getClose() != null) {
                lastRealClose = cr.getClose();
            }
        }
    }

    private void applyZScoreFlag(CleanedRecord cr, BigDecimal lastRealClose, double mean, double stdDev) {
        double logReturn = computeLogReturn(lastRealClose, cr.getClose());
        if (!Double.isFinite(logReturn)) {
            return;
        }
        double zScore = (logReturn - mean) / stdDev;
        if (Math.abs(zScore) <= Z_THRESHOLD) {
            return;
        }
        if (isEligibleForFlagging(cr.getDataQuality())) {
            cr.setDataQuality(DataQuality.ANOMALY_FLAGGED);
            cr.setAnomalyType(EXTREME_RETURN);
        }
    }

    private boolean isEligibleForFlagging(DataQuality quality) {
        return quality == DataQuality.CLEAN || quality == DataQuality.ANOMALY_CORRECTED;
    }

    private double computeLogReturn(BigDecimal prevClose, BigDecimal currClose) {
        double prev = prevClose.doubleValue();
        double curr = currClose.doubleValue();
        if (prev <= 0 || curr <= 0) {
            return Double.NaN;
        }
        return Math.log(curr / prev);
    }
}
