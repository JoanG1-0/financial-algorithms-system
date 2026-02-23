package com.financial.etl.service;

import com.financial.etl.dto.TransformSummary;
import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.entity.PriceRecord;
import com.financial.etl.repository.CleanedRecordRepository;
import com.financial.etl.repository.FinancialSeriesRepository;
import com.financial.etl.transform.DataTransformService;
import com.financial.etl.transform.calendar.TradingCalendarService;
import com.financial.etl.transform.cleaner.AnomalyDetector;
import com.financial.etl.transform.cleaner.MissingValueCleaner;
import com.financial.etl.transform.cleaner.OhlcConsistencyChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataTransformServiceTest {

    @Mock private FinancialSeriesRepository seriesRepository;
    @Mock private CleanedRecordRepository cleanedRecordRepository;
    @Mock private TradingCalendarService calendarService;
    @Mock private OhlcConsistencyChecker ohlcChecker;
    @Mock private MissingValueCleaner missingValueCleaner;
    @Mock private AnomalyDetector anomalyDetector;

    @InjectMocks
    private DataTransformService service;

    private FinancialSeries series;
    private List<CleanedRecord> stubRecords;

    @BeforeEach
    void setUp() {
        series = new FinancialSeries();
        series.setSymbol("AAPL");

        PriceRecord pr = new PriceRecord();
        pr.setDatetime(LocalDate.of(2024, 1, 2));
        pr.setOpen(BigDecimal.valueOf(100));
        pr.setHigh(BigDecimal.valueOf(110));
        pr.setLow(BigDecimal.valueOf(90));
        pr.setClose(BigDecimal.valueOf(105));
        pr.setVolume(1000L);
        series.getPriceRecords().add(pr);

        CleanedRecord cr = new CleanedRecord();
        cr.setSymbol("AAPL");
        cr.setDate(LocalDate.of(2024, 1, 2));
        cr.setClose(BigDecimal.valueOf(105));
        cr.setDataQuality(DataQuality.CLEAN);

        stubRecords = new ArrayList<>(List.of(cr));
    }

    @Test
    void transformAll_invokesFullPipelineAndPersists() {
        TreeSet<LocalDate> calendar = new TreeSet<>(List.of(LocalDate.of(2024, 1, 2)));

        when(seriesRepository.findAllWithPriceRecords()).thenReturn(List.of(series));
        when(calendarService.buildUnifiedCalendar(anyList())).thenReturn(calendar);
        when(ohlcChecker.validate(anyString(), anyList())).thenReturn(stubRecords);
        when(missingValueCleaner.fill(anyString(), any(), anyList())).thenReturn(stubRecords);
        when(anomalyDetector.detectExtremeReturns(anyList())).thenReturn(stubRecords);

        TransformSummary summary = service.transformAll();

        verify(seriesRepository).findAllWithPriceRecords();
        verify(calendarService).buildUnifiedCalendar(anyList());
        verify(ohlcChecker).validate(eq("AAPL"), anyList());
        verify(missingValueCleaner).fill(eq("AAPL"), any(), anyList());
        verify(anomalyDetector).detectExtremeReturns(anyList());
        verify(cleanedRecordRepository).deleteAll();
        verify(cleanedRecordRepository).saveAll(anyList());

        assertThat(summary.getSymbolsProcessed()).isEqualTo(1);
        assertThat(summary.getCalendarDays()).isEqualTo(1);
        assertThat(summary.getTotalRecords()).isEqualTo(1);
        assertThat(summary.getCleanRecords()).isEqualTo(1);
        assertThat(summary.getTransformedAt()).isNotNull();
    }

    @Test
    void transformAll_noSeries_returnsSummaryWithZeros() {
        when(seriesRepository.findAllWithPriceRecords()).thenReturn(List.of());
        when(calendarService.buildUnifiedCalendar(anyList())).thenReturn(new TreeSet<>());

        TransformSummary summary = service.transformAll();

        assertThat(summary.getSymbolsProcessed()).isEqualTo(0);
        assertThat(summary.getTotalRecords()).isEqualTo(0);
        verify(cleanedRecordRepository).deleteAll();
        verify(cleanedRecordRepository).saveAll(argThat(it -> !it.iterator().hasNext()));
    }

    @Test
    void transformAll_countsQualitiesCorrectly() {
        TreeSet<LocalDate> calendar = new TreeSet<>(List.of(
                LocalDate.of(2024, 1, 2),
                LocalDate.of(2024, 1, 3)
        ));

        CleanedRecord filled = new CleanedRecord();
        filled.setDataQuality(DataQuality.FORWARD_FILLED);

        List<CleanedRecord> mixed = new ArrayList<>(List.of(stubRecords.get(0), filled));

        when(seriesRepository.findAllWithPriceRecords()).thenReturn(List.of(series));
        when(calendarService.buildUnifiedCalendar(anyList())).thenReturn(calendar);
        when(ohlcChecker.validate(anyString(), anyList())).thenReturn(stubRecords);
        when(missingValueCleaner.fill(anyString(), any(), anyList())).thenReturn(mixed);
        when(anomalyDetector.detectExtremeReturns(anyList())).thenReturn(mixed);

        TransformSummary summary = service.transformAll();

        assertThat(summary.getCleanRecords()).isEqualTo(1);
        assertThat(summary.getForwardFilledRecords()).isEqualTo(1);
        assertThat(summary.getTotalRecords()).isEqualTo(2);
    }
}
