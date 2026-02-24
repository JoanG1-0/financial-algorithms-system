package com.financial.etl.transform.cleaner;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import com.financial.etl.entity.PriceRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates and corrects OHLC consistency for each price record.
 *
 * Rules:
 * - Price <= 0 → ANOMALY_FLAGGED, anomalyType = ZERO_PRICE
 * - low > high → swap low↔high → ANOMALY_CORRECTED, anomalyType = OHLC_INCONSISTENCY
 * - Other OHLC violation (e.g. open/close outside [low,high]) → ANOMALY_FLAGGED, anomalyType = OHLC_INCONSISTENCY
 * - Valid → CLEAN
 */
@Service
public class OhlcConsistencyChecker {

    public static final String OHLC_INCONSISTENCY = "OHLC_INCONSISTENCY";
    public static final String ZERO_PRICE = "ZERO_PRICE";

    /**
     * Validates each price record and produces a CleanedRecord with quality metadata.
     *
     * @param symbol  the symbol being processed
     * @param records raw price records for the symbol
     * @return list of CleanedRecord (one per input record, not yet persisted)
     */
    public List<CleanedRecord> validate(String symbol, List<PriceRecord> records) {
        List<CleanedRecord> result = new ArrayList<>(records.size());
        LocalDateTime now = LocalDateTime.now();

        for (PriceRecord pr : records) {
            CleanedRecord cr = new CleanedRecord();
            cr.setSymbol(symbol);
            cr.setDate(pr.getDatetime());
            cr.setOpen(pr.getOpen());
            cr.setHigh(pr.getHigh());
            cr.setLow(pr.getLow());
            cr.setClose(pr.getClose());
            cr.setVolume(pr.getVolume());
            cr.setTransformedAt(now);

            if (hasZeroOrNegativePrice(pr)) {
                cr.setDataQuality(DataQuality.ANOMALY_FLAGGED);
                cr.setAnomalyType(ZERO_PRICE);
            } else if (isSimpleInversion(pr)) {
                // low > high: swap to correct
                cr.setLow(pr.getHigh());
                cr.setHigh(pr.getLow());
                cr.setOriginalClose(pr.getClose());
                cr.setDataQuality(DataQuality.ANOMALY_CORRECTED);
                cr.setAnomalyType(OHLC_INCONSISTENCY);
            } else if (hasOhlcViolation(pr)) {
                cr.setDataQuality(DataQuality.ANOMALY_FLAGGED);
                cr.setAnomalyType(OHLC_INCONSISTENCY);
            } else {
                cr.setDataQuality(DataQuality.CLEAN);
            }

            result.add(cr);
        }

        return result;
    }

    private boolean hasZeroOrNegativePrice(PriceRecord pr) {
        return isZeroOrNegative(pr.getOpen())
                || isZeroOrNegative(pr.getHigh())
                || isZeroOrNegative(pr.getLow())
                || isZeroOrNegative(pr.getClose());
    }

    private boolean isZeroOrNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Simple inversion: low > high (and both non-null/positive).
     */
    private boolean isSimpleInversion(PriceRecord pr) {
        return pr.getLow() != null && pr.getHigh() != null
                && pr.getLow().compareTo(pr.getHigh()) > 0;
    }

    /**
     * Any remaining OHLC violation: open or close outside [low, high].
     */
    private boolean hasOhlcViolation(PriceRecord pr) {
        if (pr.getLow() == null || pr.getHigh() == null) {
            return false;
        }
        BigDecimal low = pr.getLow();
        BigDecimal high = pr.getHigh();

        if (pr.getOpen() != null && (pr.getOpen().compareTo(low) < 0 || pr.getOpen().compareTo(high) > 0)) {
            return true;
        }
        return pr.getClose() != null && (pr.getClose().compareTo(low) < 0 || pr.getClose().compareTo(high) > 0);
    }
}
