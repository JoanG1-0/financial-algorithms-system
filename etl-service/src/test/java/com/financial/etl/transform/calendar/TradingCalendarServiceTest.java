package com.financial.etl.transform.calendar;

import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.entity.PriceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class TradingCalendarServiceTest {

    private TradingCalendarService service;

    @BeforeEach
    void setUp() {
        service = new TradingCalendarService();
    }

    @Test
    void buildUnifiedCalendar_excludesWeekends() {
        // 2024-01-06 Saturday, 2024-01-07 Sunday, 2024-01-08 Monday
        FinancialSeries series = seriesWithDates(
                LocalDate.of(2024, 1, 6),
                LocalDate.of(2024, 1, 7),
                LocalDate.of(2024, 1, 8)
        );

        TreeSet<LocalDate> calendar = service.buildUnifiedCalendar(List.of(series));

        assertThat(calendar).containsExactly(LocalDate.of(2024, 1, 8));
    }

    @Test
    void buildUnifiedCalendar_mergesDatesFromMultipleSeries() {
        LocalDate day1 = LocalDate.of(2024, 1, 2); // Tuesday
        LocalDate day2 = LocalDate.of(2024, 1, 3); // Wednesday
        LocalDate day3 = LocalDate.of(2024, 1, 4); // Thursday

        FinancialSeries s1 = seriesWithDates(day1, day2);
        FinancialSeries s2 = seriesWithDates(day2, day3);

        TreeSet<LocalDate> calendar = service.buildUnifiedCalendar(List.of(s1, s2));

        assertThat(calendar).containsExactly(day1, day2, day3);
    }

    @Test
    void buildUnifiedCalendar_deduplicatesDates() {
        LocalDate day = LocalDate.of(2024, 1, 2);
        FinancialSeries s1 = seriesWithDates(day);
        FinancialSeries s2 = seriesWithDates(day);

        TreeSet<LocalDate> calendar = service.buildUnifiedCalendar(List.of(s1, s2));

        assertThat(calendar).hasSize(1);
    }

    @Test
    void buildUnifiedCalendar_emptySeriesListReturnsEmptySet() {
        TreeSet<LocalDate> calendar = service.buildUnifiedCalendar(List.of());
        assertThat(calendar).isEmpty();
    }

    @Test
    void buildUnifiedCalendar_seriesWithNoRecordsIsIgnored() {
        FinancialSeries empty = new FinancialSeries();
        empty.setSymbol("EMPTY");

        TreeSet<LocalDate> calendar = service.buildUnifiedCalendar(List.of(empty));
        assertThat(calendar).isEmpty();
    }

    @Test
    void buildUnifiedCalendar_resultIsAscendingSorted() {
        LocalDate d1 = LocalDate.of(2024, 1, 2);
        LocalDate d2 = LocalDate.of(2024, 1, 3);
        LocalDate d3 = LocalDate.of(2024, 1, 4);

        FinancialSeries s = seriesWithDates(d3, d1, d2);
        TreeSet<LocalDate> calendar = service.buildUnifiedCalendar(List.of(s));

        assertThat(calendar).containsExactly(d1, d2, d3);
    }

    // Helper: build a FinancialSeries with PriceRecords at the given dates
    private FinancialSeries seriesWithDates(LocalDate... dates) {
        FinancialSeries series = new FinancialSeries();
        series.setSymbol("TEST");
        List<PriceRecord> records = new ArrayList<>();
        for (LocalDate date : dates) {
            PriceRecord pr = new PriceRecord();
            pr.setDatetime(date);
            pr.setSeries(series);
            records.add(pr);
        }
        series.getPriceRecords().addAll(records);
        return series;
    }
}
