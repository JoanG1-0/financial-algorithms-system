package com.financial.etl.transform.cleaner;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class MissingValueCleanerTest {

    private MissingValueCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new MissingValueCleaner();
    }

    @Test
    void fill_noGaps_returnsOriginalRecords() {
        LocalDate d1 = LocalDate.of(2024, 1, 2);
        LocalDate d2 = LocalDate.of(2024, 1, 3);
        TreeSet<LocalDate> calendar = calendarOf(d1, d2);

        List<CleanedRecord> records = List.of(
                cleanRecord("AAPL", d1, "100"),
                cleanRecord("AAPL", d2, "101")
        );

        List<CleanedRecord> result = cleaner.fill("AAPL", calendar, records);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDataQuality()).isEqualTo(DataQuality.CLEAN);
        assertThat(result.get(1).getDataQuality()).isEqualTo(DataQuality.CLEAN);
    }

    @Test
    void fill_missingDateInMiddle_forwardFilled() {
        LocalDate d1 = LocalDate.of(2024, 1, 2);
        LocalDate d2 = LocalDate.of(2024, 1, 3); // gap
        LocalDate d3 = LocalDate.of(2024, 1, 4);
        TreeSet<LocalDate> calendar = calendarOf(d1, d2, d3);

        List<CleanedRecord> records = List.of(
                cleanRecord("AAPL", d1, "100"),
                cleanRecord("AAPL", d3, "102")
        );

        List<CleanedRecord> result = cleaner.fill("AAPL", calendar, records);

        assertThat(result).hasSize(3);
        CleanedRecord filled = result.get(1);
        assertThat(filled.getDate()).isEqualTo(d2);
        assertThat(filled.getDataQuality()).isEqualTo(DataQuality.FORWARD_FILLED);
        assertThat(filled.getClose()).isEqualByComparingTo("100"); // carries d1 close
    }

    @Test
    void fill_gapAtStart_backwardFilled() {
        LocalDate d1 = LocalDate.of(2024, 1, 2); // no record for this symbol
        LocalDate d2 = LocalDate.of(2024, 1, 3);
        TreeSet<LocalDate> calendar = calendarOf(d1, d2);

        List<CleanedRecord> records = List.of(
                cleanRecord("LSE", d2, "50")
        );

        List<CleanedRecord> result = cleaner.fill("LSE", calendar, records);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(d1);
        assertThat(result.get(0).getDataQuality()).isEqualTo(DataQuality.FORWARD_FILLED);
        assertThat(result.get(0).getClose()).isEqualByComparingTo("50");
    }

    @Test
    void fill_symbolHasNoRecordsAtAll_returnsEmpty() {
        TreeSet<LocalDate> calendar = calendarOf(
                LocalDate.of(2024, 1, 2),
                LocalDate.of(2024, 1, 3)
        );

        List<CleanedRecord> result = cleaner.fill("GHOST", calendar, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void fill_emptyCalendar_returnsEmpty() {
        List<CleanedRecord> records = List.of(cleanRecord("AAPL", LocalDate.of(2024, 1, 2), "100"));
        List<CleanedRecord> result = cleaner.fill("AAPL", new TreeSet<>(), records);

        assertThat(result).isEmpty();
    }

    @Test
    void fill_preservesQualityOfExistingRecords() {
        LocalDate d1 = LocalDate.of(2024, 1, 2);
        TreeSet<LocalDate> calendar = calendarOf(d1);

        CleanedRecord cr = cleanRecord("AAPL", d1, "100");
        cr.setDataQuality(DataQuality.ANOMALY_CORRECTED);
        cr.setAnomalyType("OHLC_INCONSISTENCY");

        List<CleanedRecord> result = cleaner.fill("AAPL", calendar, List.of(cr));

        assertThat(result.get(0).getDataQuality()).isEqualTo(DataQuality.ANOMALY_CORRECTED);
        assertThat(result.get(0).getAnomalyType()).isEqualTo("OHLC_INCONSISTENCY");
    }

    // Helpers
    private TreeSet<LocalDate> calendarOf(LocalDate... dates) {
        TreeSet<LocalDate> set = new TreeSet<>();
        for (LocalDate d : dates) {
            set.add(d);
        }
        return set;
    }

    private CleanedRecord cleanRecord(String symbol, LocalDate date, String close) {
        CleanedRecord cr = new CleanedRecord();
        cr.setSymbol(symbol);
        cr.setDate(date);
        cr.setOpen(new BigDecimal(close));
        cr.setHigh(new BigDecimal(close));
        cr.setLow(new BigDecimal(close));
        cr.setClose(new BigDecimal(close));
        cr.setVolume(1000L);
        cr.setDataQuality(DataQuality.CLEAN);
        return cr;
    }
}
