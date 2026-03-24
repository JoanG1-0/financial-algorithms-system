package com.financial.etl.sorting;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ComparatorTest {

    private final ActivoComparator activoCmp = new ActivoComparator();
    private final VolumeComparator volumeCmp = new VolumeComparator();

    // -----------------------------------------------------------------------
    // ActivoComparator
    // -----------------------------------------------------------------------

    @Test
    void activoComparator_earlierDateComesFirst() {
        CleanedRecord a = record(LocalDate.of(2023, 1, 1), 100.0, 1000L);
        CleanedRecord b = record(LocalDate.of(2023, 1, 2), 100.0, 1000L);
        assertTrue(activoCmp.compare(a, b) < 0);
    }

    @Test
    void activoComparator_laterDateComesSecond() {
        CleanedRecord a = record(LocalDate.of(2023, 6, 15), 100.0, 500L);
        CleanedRecord b = record(LocalDate.of(2023, 1, 1), 100.0, 500L);
        assertTrue(activoCmp.compare(a, b) > 0);
    }

    @Test
    void activoComparator_sameDateLowerCloseComesFirst() {
        CleanedRecord a = record(LocalDate.of(2023, 3, 10), 90.0, 1000L);
        CleanedRecord b = record(LocalDate.of(2023, 3, 10), 120.0, 1000L);
        assertTrue(activoCmp.compare(a, b) < 0);
    }

    @Test
    void activoComparator_sameDateHigherCloseComesSecond() {
        CleanedRecord a = record(LocalDate.of(2023, 3, 10), 200.0, 1000L);
        CleanedRecord b = record(LocalDate.of(2023, 3, 10), 150.0, 1000L);
        assertTrue(activoCmp.compare(a, b) > 0);
    }

    @Test
    void activoComparator_sameDateSameCloseIsEqual() {
        CleanedRecord a = record(LocalDate.of(2023, 3, 10), 100.0, 500L);
        CleanedRecord b = record(LocalDate.of(2023, 3, 10), 100.0, 999L);
        assertEquals(0, activoCmp.compare(a, b));
    }

    // -----------------------------------------------------------------------
    // VolumeComparator
    // -----------------------------------------------------------------------

    @Test
    void volumeComparator_lowerVolumeComesFirst() {
        CleanedRecord a = record(LocalDate.of(2023, 1, 1), 100.0, 500L);
        CleanedRecord b = record(LocalDate.of(2023, 1, 1), 100.0, 2000L);
        assertTrue(volumeCmp.compare(a, b) < 0);
    }

    @Test
    void volumeComparator_higherVolumeComesSecond() {
        CleanedRecord a = record(LocalDate.of(2023, 1, 1), 100.0, 9000L);
        CleanedRecord b = record(LocalDate.of(2023, 1, 1), 100.0, 100L);
        assertTrue(volumeCmp.compare(a, b) > 0);
    }

    @Test
    void volumeComparator_equalVolumesAreEqual() {
        CleanedRecord a = record(LocalDate.of(2023, 1, 1), 100.0, 5000L);
        CleanedRecord b = record(LocalDate.of(2023, 6, 1), 200.0, 5000L);
        assertEquals(0, volumeCmp.compare(a, b));
    }

    @Test
    void volumeComparator_nullVolumeTreatedAsMinValue() {
        CleanedRecord a = record(LocalDate.of(2023, 1, 1), 100.0, null);
        CleanedRecord b = record(LocalDate.of(2023, 1, 1), 100.0, 1L);
        assertTrue(volumeCmp.compare(a, b) < 0);
    }

    @Test
    void volumeComparator_bothNullVolumesAreEqual() {
        CleanedRecord a = record(LocalDate.of(2023, 1, 1), 100.0, null);
        CleanedRecord b = record(LocalDate.of(2023, 6, 1), 200.0, null);
        assertEquals(0, volumeCmp.compare(a, b));
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private static CleanedRecord record(LocalDate date, double close, Long volume) {
        CleanedRecord r = new CleanedRecord();
        r.setSymbol("TEST");
        r.setDate(date);
        r.setClose(BigDecimal.valueOf(close));
        r.setOpen(BigDecimal.valueOf(close));
        r.setHigh(BigDecimal.valueOf(close));
        r.setLow(BigDecimal.valueOf(close));
        r.setVolume(volume);
        r.setDataQuality(DataQuality.CLEAN);
        r.setTransformedAt(LocalDateTime.now());
        return r;
    }
}
