package com.financial.etl.transform.cleaner;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeMap;

/**
 * Fills missing dates in a symbol's series using the unified trading calendar.
 *
 * Strategy:
 * - For each date in the unified calendar:
 *   · If the symbol has a record → keep it (preserving its quality from OhlcConsistencyChecker)
 *   · If missing → forward-fill from the last known record → FORWARD_FILLED
 * - If no prior record exists (gap at start) → backward-fill from the first known record → FORWARD_FILLED
 *
 * Complexity: O(D) per symbol.
 */
@Service
public class MissingValueCleaner {

    /**
     * Aligns a symbol's validated records to the unified calendar.
     *
     * @param symbol          the trading symbol
     * @param calendarDates   unified sorted set of trading dates
     * @param validatedRecords output of OhlcConsistencyChecker (may have gaps)
     * @return aligned list (one record per calendar date)
     */
    public List<CleanedRecord> fill(String symbol,
                                    NavigableSet<LocalDate> calendarDates,
                                    List<CleanedRecord> validatedRecords) {

        // Index existing records by date for O(1) lookup
        TreeMap<LocalDate, CleanedRecord> byDate = new TreeMap<>();
        for (CleanedRecord cr : validatedRecords) {
            byDate.put(cr.getDate(), cr);
        }

        List<CleanedRecord> aligned = new ArrayList<>(calendarDates.size());
        CleanedRecord lastKnown = null;

        for (LocalDate date : calendarDates) {
            CleanedRecord existing = byDate.get(date);

            if (existing != null) {
                lastKnown = existing;
                aligned.add(existing);
            } else {
                // Need to fill this date
                CleanedRecord fill;
                if (lastKnown != null) {
                    // Forward-fill from lastKnown
                    fill = forwardFill(symbol, date, lastKnown);
                } else {
                    // No prior record yet; try to find the first future record for backward-fill
                    CleanedRecord firstFuture = findFirstFuture(byDate, date);
                    if (firstFuture != null) {
                        fill = backwardFill(symbol, date, firstFuture);
                    } else {
                        // No data at all for this symbol — skip
                        continue;
                    }
                }
                aligned.add(fill);
            }
        }

        return aligned;
    }

    private CleanedRecord forwardFill(String symbol, LocalDate date, CleanedRecord source) {
        CleanedRecord cr = new CleanedRecord();
        cr.setSymbol(symbol);
        cr.setDate(date);
        cr.setOpen(source.getOpen());
        cr.setHigh(source.getHigh());
        cr.setLow(source.getLow());
        cr.setClose(source.getClose());
        cr.setVolume(source.getVolume());
        cr.setDataQuality(DataQuality.FORWARD_FILLED);
        cr.setTransformedAt(LocalDateTime.now());
        return cr;
    }

    private CleanedRecord backwardFill(String symbol, LocalDate date, CleanedRecord source) {
        return forwardFill(symbol, date, source);
    }

    private CleanedRecord findFirstFuture(TreeMap<LocalDate, CleanedRecord> byDate, LocalDate date) {
        var entry = byDate.higherEntry(date);
        return entry != null ? entry.getValue() : null;
    }
}
