package com.financial.etl.transform;

import com.financial.etl.dto.TransformSummary;
import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.repository.CleanedRecordRepository;
import com.financial.etl.repository.FinancialSeriesRepository;
import com.financial.etl.transform.calendar.TradingCalendarService;
import com.financial.etl.transform.cleaner.AnomalyDetector;
import com.financial.etl.transform.cleaner.MissingValueCleaner;
import com.financial.etl.transform.cleaner.OhlcConsistencyChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Orchestrates the full data transformation pipeline.
 *
 * Pipeline:
 * 1. Build unified trading calendar from all observed dates (excl. weekends)
 * 2. Per symbol: OHLC validation → gap filling → anomaly detection
 * 3. Persist all CleanedRecords (replace previous run)
 */
@Service
public class DataTransformService {

    private final FinancialSeriesRepository seriesRepository;
    private final CleanedRecordRepository cleanedRecordRepository;
    private final TradingCalendarService calendarService;
    private final OhlcConsistencyChecker ohlcChecker;
    private final MissingValueCleaner missingValueCleaner;
    private final AnomalyDetector anomalyDetector;

    public DataTransformService(FinancialSeriesRepository seriesRepository,
                                CleanedRecordRepository cleanedRecordRepository,
                                TradingCalendarService calendarService,
                                OhlcConsistencyChecker ohlcChecker,
                                MissingValueCleaner missingValueCleaner,
                                AnomalyDetector anomalyDetector) {
        this.seriesRepository = seriesRepository;
        this.cleanedRecordRepository = cleanedRecordRepository;
        this.calendarService = calendarService;
        this.ohlcChecker = ohlcChecker;
        this.missingValueCleaner = missingValueCleaner;
        this.anomalyDetector = anomalyDetector;
    }

    /**
     * Executes the full transformation pipeline over all available series.
     *
     * @return summary statistics of the transformation run
     */
    @Transactional
    public TransformSummary transformAll() {
        // Load all series with their price records
        List<FinancialSeries> allSeries = seriesRepository.findAllWithPriceRecords();

        // Step 1: Build unified trading calendar
        TreeSet<LocalDate> calendar = calendarService.buildUnifiedCalendar(allSeries);

        // Step 2: Process each symbol
        List<CleanedRecord> allCleanedRecords = new ArrayList<>();

        for (FinancialSeries series : allSeries) {
            String symbol = series.getSymbol();

            // 2a. OHLC consistency check
            List<CleanedRecord> validated = ohlcChecker.validate(symbol, series.getPriceRecords());

            // 2b. Fill missing dates using unified calendar
            List<CleanedRecord> aligned = missingValueCleaner.fill(symbol, calendar, validated);

            // 2c. Detect extreme returns
            List<CleanedRecord> finalRecords = anomalyDetector.detectExtremeReturns(aligned);

            allCleanedRecords.addAll(finalRecords);
        }

        // Step 3: Persist (replace previous run)
        // flush() forces Hibernate to send DELETE SQL before INSERT to avoid
        // unique constraint (symbol, date) violations when re-running the pipeline
        cleanedRecordRepository.deleteAll();
        cleanedRecordRepository.flush();
        cleanedRecordRepository.saveAll(allCleanedRecords);

        return buildSummary(allSeries.size(), calendar.size(), allCleanedRecords);
    }

    private TransformSummary buildSummary(int symbolCount, int calendarDays,
                                           List<CleanedRecord> records) {
        long total = records.size();
        long clean = count(records, DataQuality.CLEAN);
        long filled = count(records, DataQuality.FORWARD_FILLED);
        long corrected = count(records, DataQuality.ANOMALY_CORRECTED);
        long flagged = count(records, DataQuality.ANOMALY_FLAGGED);

        return new TransformSummary(symbolCount, calendarDays, total,
                clean, filled, corrected, flagged, LocalDateTime.now());
    }

    private long count(List<CleanedRecord> records, DataQuality quality) {
        return records.stream().filter(r -> r.getDataQuality() == quality).count();
    }
}
