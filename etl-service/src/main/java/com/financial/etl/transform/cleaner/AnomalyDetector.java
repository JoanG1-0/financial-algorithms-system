package com.financial.etl.transform.cleaner;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detects extreme price returns using Z-score analysis on logarithmic returns.
 *
 * Algorithm:
 * 1. Compute log-returns only over consecutive NON-forward-filled records
 *    r_t = ln(close_t / close_{t-1})
 * 2. Compute μ and σ over those returns
 * 3. For each record (including FORWARD_FILLED), compute z-score using the last known real close
 * 4. If |z| > Z_THRESHOLD and record is CLEAN or ANOMALY_CORRECTED → ANOMALY_FLAGGED
 *
 * Complexity: O(D) per symbol, 2 passes.
 */
@Service
public class AnomalyDetector {

    static final double Z_THRESHOLD = 3.0;

    public static final String EXTREME_RETURN = "EXTREME_RETURN";

    /**
     * Marks records with extreme log-returns as ANOMALY_FLAGGED.
     * Does NOT modify records already flagged for other reasons.
     *
     * @param records aligned records for a single symbol (from MissingValueCleaner)
     * @return the same list, mutated in-place where anomalies are detected
     */
    public List<CleanedRecord> detectExtremeReturns(List<CleanedRecord> records) {
        if (records.size() < 2) {
            return records;
        }

        // Pass 1: collect log-returns from non-forward-filled consecutive pairs
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
                // forward-filled: don't update prevClose for return computation
                prevWasReal = false;
            }
        }

        if (count < 2) {
            return records;
        }

        double mean = sumR / count;
        double variance = (sumR2 / count) - (mean * mean);
        double stdDev = Math.sqrt(Math.max(variance, 0.0));

        if (stdDev == 0.0) {
            return records;
        }

        // Pass 2: flag extreme returns
        BigDecimal lastRealClose = null;

        for (CleanedRecord cr : records) {
            boolean isReal = cr.getDataQuality() != DataQuality.FORWARD_FILLED;

            if (lastRealClose != null && cr.getClose() != null) {
                double logReturn = computeLogReturn(lastRealClose, cr.getClose());
                if (Double.isFinite(logReturn)) {
                    double zScore = (logReturn - mean) / stdDev;
                    if (Math.abs(zScore) > Z_THRESHOLD) {
                        DataQuality quality = cr.getDataQuality();
                        if (quality == DataQuality.CLEAN || quality == DataQuality.ANOMALY_CORRECTED) {
                            cr.setDataQuality(DataQuality.ANOMALY_FLAGGED);
                            cr.setAnomalyType(EXTREME_RETURN);
                        }
                    }
                }
            }

            if (isReal && cr.getClose() != null) {
                lastRealClose = cr.getClose();
            }
        }

        return records;
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
