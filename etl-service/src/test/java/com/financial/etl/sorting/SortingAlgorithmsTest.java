package com.financial.etl.sorting;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.ToLongFunction;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SortingAlgorithmsTest {

    private static final Comparator<CleanedRecord> DATE_CMP = new ActivoComparator();
    private static final Comparator<CleanedRecord> VOL_CMP  = new VolumeComparator();
    private static final ToLongFunction<CleanedRecord> DATE_KEY = r -> r.getDate().toEpochDay();
    private static final ToLongFunction<CleanedRecord> VOL_KEY  = r -> r.getVolume() != null ? r.getVolume() : 0L;

    private CleanedRecord[] master;

    @BeforeEach
    void setUp() {
        // 10 registros con fechas y closes variados
        master = new CleanedRecord[]{
            record("AAPL", "2023-01-05", 152.0, 1000L),
            record("AAPL", "2023-01-03", 130.0, 5000L),
            record("AAPL", "2023-01-01", 150.0, 3000L),
            record("AAPL", "2023-01-04", 140.0, 2000L),
            record("AAPL", "2023-01-02", 160.0, 8000L),
            record("AAPL", "2023-01-08", 120.0, 500L),
            record("AAPL", "2023-01-06", 145.0, 4000L),
            record("AAPL", "2023-01-07", 135.0, 7000L),
            record("AAPL", "2023-01-09", 155.0, 100L),
            record("AAPL", "2023-01-10", 125.0, 9000L),
        };
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static CleanedRecord record(String symbol, String date, double close, long volume) {
        CleanedRecord r = new CleanedRecord();
        r.setSymbol(symbol);
        r.setDate(LocalDate.parse(date));
        r.setClose(BigDecimal.valueOf(close));
        r.setVolume(volume);
        r.setOpen(BigDecimal.valueOf(close));
        r.setHigh(BigDecimal.valueOf(close));
        r.setLow(BigDecimal.valueOf(close));
        r.setDataQuality(DataQuality.CLEAN);
        r.setTransformedAt(LocalDateTime.now());
        return r;
    }

    private CleanedRecord[] copy() {
        return Arrays.copyOf(master, master.length);
    }

    private static boolean isSorted(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        for (int i = 1; i < a.length; i++)
            if (cmp.compare(a[i - 1], a[i]) > 0) return false;
        return true;
    }

    // -----------------------------------------------------------------------
    // Tests — un test por algoritmo, misma verificación: isSorted
    // -----------------------------------------------------------------------

    @Test
    void timSort_sortsByDateClose() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.timSort(a, DATE_CMP);
        assertTrue(isSorted(a, DATE_CMP));
    }

    @Test
    void combSort_sortsByDateClose() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.combSort(a, DATE_CMP);
        assertTrue(isSorted(a, DATE_CMP));
    }

    @Test
    void selectionSort_sortsByVolume() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.selectionSort(a, VOL_CMP);
        assertTrue(isSorted(a, VOL_CMP));
    }

    @Test
    void treeSort_sortsByDateClose() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.treeSort(a, DATE_CMP);
        assertTrue(isSorted(a, DATE_CMP));
    }

    @Test
    void pigeonholeSort_sortsByDateClose() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.pigeonholeSort(a, DATE_CMP, DATE_KEY);
        assertTrue(isSorted(a, DATE_CMP));
    }

    @Test
    void bucketSort_sortsByDateClose() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.bucketSort(a, DATE_CMP, DATE_KEY);
        assertTrue(isSorted(a, DATE_CMP));
    }

    @Test
    void quickSort_sortsByDateClose() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.quickSort(a, DATE_CMP);
        assertTrue(isSorted(a, DATE_CMP));
    }

    @Test
    void heapSort_sortsByVolume() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.heapSort(a, VOL_CMP);
        assertTrue(isSorted(a, VOL_CMP));
    }

    @Test
    void bitonicSort_sortsByDateClose() {
        // Preparar array de potencia de 2 con sentinela
        CleanedRecord[] a = copy();
        int realN = a.length;
        int p = Integer.highestOneBit(realN - 1) << 1;
        CleanedRecord sentinel = record("__S__", "9999-12-31", Double.MAX_VALUE, Long.MAX_VALUE);
        CleanedRecord[] padded = Arrays.copyOf(a, p);
        Arrays.fill(padded, realN, p, sentinel);
        SortingAlgorithms.bitonicSort(padded, DATE_CMP);
        CleanedRecord[] result = Arrays.copyOf(padded, realN);
        assertTrue(isSorted(result, DATE_CMP));
    }

    @Test
    void gnomeSort_sortsByDateClose() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.gnomeSort(a, DATE_CMP);
        assertTrue(isSorted(a, DATE_CMP));
    }

    @Test
    void binaryInsertionSort_sortsByDateClose() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.binaryInsertionSort(a, DATE_CMP);
        assertTrue(isSorted(a, DATE_CMP));
    }

    @Test
    void radixSort_sortsByDateClose() {
        CleanedRecord[] a = copy();
        SortingAlgorithms.radixSort(a, DATE_CMP, DATE_KEY);
        assertTrue(isSorted(a, DATE_CMP));
    }
}
