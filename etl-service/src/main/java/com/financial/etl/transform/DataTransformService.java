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
import java.util.NavigableSet;

/**
 * Orquesta el pipeline completo de transformación de datos.
 *
 * Pipeline:
 * 1. Construir calendario bursátil unificado a partir de todas las fechas observadas (excl. fines de semana)
 * 2. Por activo: validación OHLC → relleno de huecos → detección de anomalías
 * 3. Persistir todos los CleanedRecords (reemplaza la ejecución anterior)
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
     * Ejecuta el pipeline completo de transformación sobre todas las series disponibles.
     *
     * @return estadísticas resumen de la ejecución de transformación
     */
    @Transactional
    public TransformSummary transformAll() {
        // Carga todas las series con sus registros de precio
        List<FinancialSeries> allSeries = seriesRepository.findAllWithPriceRecords();

        // Paso 1: Construir calendario bursátil unificado
        NavigableSet<LocalDate> calendar = calendarService.buildUnifiedCalendar(allSeries);

        // Paso 2: Procesar cada activo
        List<CleanedRecord> allCleanedRecords = new ArrayList<>();

        for (FinancialSeries series : allSeries) {
            String symbol = series.getSymbol();

            // 2a. Verificación de consistencia OHLC
            List<CleanedRecord> validated = ohlcChecker.validate(symbol, series.getPriceRecords());

            // 2b. Relleno de fechas faltantes usando el calendario unificado
            List<CleanedRecord> aligned = missingValueCleaner.fill(symbol, calendar, validated);

            // 2c. Detección de retornos extremos
            List<CleanedRecord> finalRecords = anomalyDetector.detectExtremeReturns(aligned);

            allCleanedRecords.addAll(finalRecords);
        }

        // Paso 3: Persistir (reemplaza la ejecución anterior)
        // flush() fuerza a Hibernate a enviar el DELETE SQL antes del INSERT para evitar
        // violaciones de restricción única (symbol, date) al re-ejecutar el pipeline
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

        return TransformSummary.builder()
                .symbolsProcessed(symbolCount)
                .calendarDays(calendarDays)
                .totalRecords(total)
                .cleanRecords(clean)
                .forwardFilledRecords(filled)
                .anomalyCorrectedRecords(corrected)
                .anomalyFlaggedRecords(flagged)
                .transformedAt(LocalDateTime.now())
                .build();
    }

    private long count(List<CleanedRecord> records, DataQuality quality) {
        return records.stream().filter(r -> r.getDataQuality() == quality).count();
    }
}
