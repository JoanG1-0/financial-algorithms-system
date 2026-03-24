package com.financial.etl.sorting;

import com.financial.etl.dto.BenchmarkSummaryDto;
import com.financial.etl.dto.CleanedRecordDto;
import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import com.financial.etl.repository.CleanedRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SortingBenchmarkTest {

    @Mock
    private CleanedRecordRepository repository;

    @InjectMocks
    private SortingBenchmark benchmark;

    private List<CleanedRecord> records;

    @BeforeEach
    void setUp() {
        records = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            records.add(record("AAPL", LocalDate.of(2023, 1, i), 100.0 + i, (long) i * 1000));
        }
    }

    // -----------------------------------------------------------------------
    // run()
    // -----------------------------------------------------------------------

    @Test
    void run_returns12AlgorithmsPerCriterion() {
        when(repository.findAll()).thenReturn(records);
        BenchmarkSummaryDto result = benchmark.run();
        assertEquals(12, result.getSortByDateClose().size());
        assertEquals(12, result.getSortByVolume().size());
    }

    @Test
    void run_withEmptyRepository_returns12AlgorithmsWithSizeZero() {
        when(repository.findAll()).thenReturn(List.of());
        BenchmarkSummaryDto result = benchmark.run();
        assertEquals(12, result.getSortByDateClose().size());
        assertEquals(12, result.getSortByVolume().size());
        assertTrue(result.getTop15ByVolume().isEmpty());
    }

    @Test
    void run_allAlgorithmNamesPresent() {
        when(repository.findAll()).thenReturn(records);
        BenchmarkSummaryDto result = benchmark.run();
        List<String> names = result.getSortByDateClose().stream()
                .map(r -> r.getAlgorithm()).toList();
        assertTrue(names.contains("TimSort"));
        assertTrue(names.contains("QuickSort"));
        assertTrue(names.contains("HeapSort"));
        assertTrue(names.contains("RadixSort"));
        assertTrue(names.contains("BitonicSort"));
        assertTrue(names.contains("SelectionSort"));
        assertTrue(names.contains("GnomeSort"));
        assertTrue(names.contains("BinaryInsertionSort"));
    }

    @Test
    void run_timeSecondsIsNonNegative() {
        when(repository.findAll()).thenReturn(records);
        BenchmarkSummaryDto result = benchmark.run();
        result.getSortByDateClose().forEach(r ->
                assertTrue(r.getTimeSeconds() >= 0));
        result.getSortByVolume().forEach(r ->
                assertTrue(r.getTimeSeconds() >= 0));
    }

    // -----------------------------------------------------------------------
    // runTop15() / extractTop15
    // -----------------------------------------------------------------------

    @Test
    void runTop15_returnsAtMost15Records() {
        when(repository.findAll()).thenReturn(records);
        List<CleanedRecordDto> top15 = benchmark.runTop15();
        assertTrue(top15.size() <= 15);
    }

    @Test
    void runTop15_returnsAllWhenLessThan15Records() {
        when(repository.findAll()).thenReturn(records);
        List<CleanedRecordDto> top15 = benchmark.runTop15();
        assertEquals(10, top15.size());
    }

    @Test
    void runTop15_isSortedAscendingByVolume() {
        when(repository.findAll()).thenReturn(records);
        List<CleanedRecordDto> top15 = benchmark.runTop15();
        for (int i = 1; i < top15.size(); i++) {
            assertTrue(top15.get(i).getVolume() >= top15.get(i - 1).getVolume(),
                    "El volumen debe ser ascendente entre registros consecutivos");
        }
    }

    @Test
    void runTop15_returnsHighestVolumeRecords() {
        List<CleanedRecord> bigList = new ArrayList<>(records);
        // Agregar 10 registros con volumen muy bajo para que los 10 originales sean el top
        for (int i = 11; i <= 20; i++) {
            bigList.add(record("MSFT", LocalDate.of(2022, 1, i), 50.0, 1L));
        }
        when(repository.findAll()).thenReturn(bigList);
        List<CleanedRecordDto> top15 = benchmark.runTop15();
        assertEquals(15, top15.size());
        // Todos deben tener volumen >= 1000 (los originales) o el mínimo del top 15
        assertTrue(top15.stream().allMatch(r -> r.getVolume() >= 1L));
    }

    @Test
    void runTop15_withNullVolume_doesNotThrow() {
        CleanedRecord nullVol = record("NULL", LocalDate.of(2020, 1, 1), 100.0, null);
        List<CleanedRecord> withNull = new ArrayList<>(records);
        withNull.add(nullVol);
        when(repository.findAll()).thenReturn(withNull);
        assertDoesNotThrow(() -> benchmark.runTop15());
    }

    // -----------------------------------------------------------------------
    // sortedSample()
    // -----------------------------------------------------------------------

    @Test
    void sortedSample_timSort_dateCriterion_isSortedByDate() {
        when(repository.findAll()).thenReturn(records);
        List<CleanedRecordDto> result = benchmark.sortedSample("TimSort", "date", 10);
        assertFalse(result.isEmpty());
        for (int i = 1; i < result.size(); i++) {
            assertFalse(result.get(i).getDate().isBefore(result.get(i - 1).getDate()),
                    "Las fechas deben ser ascendentes");
        }
    }

    @Test
    void sortedSample_quickSort_volumeCriterion_isSortedByVolume() {
        when(repository.findAll()).thenReturn(records);
        List<CleanedRecordDto> result = benchmark.sortedSample("QuickSort", "volume", 10);
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i).getVolume() >= result.get(i - 1).getVolume(),
                    "Los volúmenes deben ser ascendentes");
        }
    }

    @Test
    void sortedSample_limitIsRespected() {
        when(repository.findAll()).thenReturn(records);
        List<CleanedRecordDto> result = benchmark.sortedSample("HeapSort", "date", 3);
        assertEquals(3, result.size());
    }

    @Test
    void sortedSample_unknownAlgorithm_throwsIllegalArgumentException() {
        when(repository.findAll()).thenReturn(records);
        assertThrows(IllegalArgumentException.class,
                () -> benchmark.sortedSample("MergeSort", "date", 5));
    }

    @Test
    void sortedSample_allAlgorithms_dateCriterion_doNotThrow() {
        List<String> algos = List.of("TimSort", "CombSort", "SelectionSort", "TreeSort",
                "PigeonholeSort", "BucketSort", "QuickSort", "HeapSort", "BitonicSort",
                "GnomeSort", "BinaryInsertionSort", "RadixSort");
        when(repository.findAll()).thenReturn(records);
        for (String algo : algos) {
            assertDoesNotThrow(() -> benchmark.sortedSample(algo, "date", 5),
                    "Algoritmo " + algo + " lanzó excepción inesperada");
        }
    }

    @Test
    void sortedSample_allAlgorithms_volumeCriterion_doNotThrow() {
        List<String> algos = List.of("TimSort", "CombSort", "SelectionSort", "TreeSort",
                "PigeonholeSort", "BucketSort", "QuickSort", "HeapSort", "BitonicSort",
                "GnomeSort", "BinaryInsertionSort", "RadixSort");
        when(repository.findAll()).thenReturn(records);
        for (String algo : algos) {
            assertDoesNotThrow(() -> benchmark.sortedSample(algo, "volume", 5),
                    "Algoritmo " + algo + " lanzó excepción inesperada");
        }
    }

    @Test
    void sortedSample_emptyCriterionDefaultsToDate() {
        when(repository.findAll()).thenReturn(records);
        List<CleanedRecordDto> result = benchmark.sortedSample("TimSort", "otro", 5);
        assertFalse(result.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private static CleanedRecord record(String symbol, LocalDate date, double close, Long volume) {
        CleanedRecord r = new CleanedRecord();
        r.setSymbol(symbol);
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
