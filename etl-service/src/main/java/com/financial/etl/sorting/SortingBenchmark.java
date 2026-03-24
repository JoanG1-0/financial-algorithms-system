package com.financial.etl.sorting;

import com.financial.etl.dto.BenchmarkSummaryDto;
import com.financial.etl.dto.CleanedRecordDto;
import com.financial.etl.dto.SortingResultDto;
import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.repository.CleanedRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;

@Service
public class SortingBenchmark {

    private static final int DATASET_SIZE = 25_100;

    private static final Comparator<CleanedRecord> DATE_CLOSE_CMP = new ActivoComparator();
    private static final Comparator<CleanedRecord> VOLUME_CMP     = new VolumeComparator();

    /** Clave numérica para criterio fecha (toEpochDay). */
    private static final ToLongFunction<CleanedRecord> DATE_KEY =
            r -> r.getDate().toEpochDay();

    /** Clave numérica para criterio volumen. */
    private static final ToLongFunction<CleanedRecord> VOL_KEY =
            r -> r.getVolume() != null ? r.getVolume() : 0L;

    private final CleanedRecordRepository repository;

    public SortingBenchmark(CleanedRecordRepository repository) {
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Punto de entrada principal
    // -------------------------------------------------------------------------

    public BenchmarkSummaryDto run() {
        CleanedRecord[] master = repository.findAll().toArray(new CleanedRecord[0]);

        List<SortingResultDto> byDateClose = runRound(master, DATE_CLOSE_CMP, DATE_KEY);
        List<SortingResultDto> byVolume    = runRound(master, VOLUME_CMP,     VOL_KEY);

        List<CleanedRecordDto> top15 = extractTop15(master);

        return new BenchmarkSummaryDto(byDateClose, byVolume, top15);
    }

    // -------------------------------------------------------------------------
    // Top 15 por volumen (usa HeapSort con VolumeComparator sobre array fresco)
    // -------------------------------------------------------------------------

    public List<CleanedRecordDto> runTop15() {
        CleanedRecord[] master = repository.findAll().toArray(new CleanedRecord[0]);
        return extractTop15(master);
    }

    // -------------------------------------------------------------------------
    // Resultado ordenado de un algoritmo específico
    // -------------------------------------------------------------------------

    /**
     * Ejecuta el algoritmo indicado con el criterio dado y devuelve los primeros
     * {@code limit} registros realmente ordenados, permitiendo verificar el orden real.
     *
     * @param algorithm nombre exacto: TimSort, CombSort, SelectionSort, TreeSort,
     *                  PigeonholeSort, BucketSort, QuickSort, HeapSort, BitonicSort,
     *                  GnomeSort, BinaryInsertionSort, RadixSort
     * @param criterion "date" (fecha ASC + close ASC) o "volume" (volumen ASC)
     * @param limit     máximo de registros a devolver
     */
    public List<CleanedRecordDto> sortedSample(String algorithm, String criterion, int limit) {
        CleanedRecord[] master = repository.findAll().toArray(new CleanedRecord[0]);
        int n = Math.min(master.length, DATASET_SIZE);

        Comparator<CleanedRecord> cmp;
        ToLongFunction<CleanedRecord> keyFn;
        if ("volume".equalsIgnoreCase(criterion)) {
            cmp    = VOLUME_CMP;
            keyFn  = VOL_KEY;
        } else {
            cmp    = DATE_CLOSE_CMP;
            keyFn  = DATE_KEY;
        }

        CleanedRecord[] arr;

        switch (algorithm) {
            case "TimSort"             -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.timSort(arr, cmp); }
            case "CombSort"            -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.combSort(arr, cmp); }
            case "SelectionSort"       -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.selectionSort(arr, cmp); }
            case "TreeSort"            -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.treeSort(arr, cmp); }
            case "PigeonholeSort"      -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.pigeonholeSort(arr, cmp, keyFn); }
            case "BucketSort"          -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.bucketSort(arr, cmp, keyFn); }
            case "QuickSort"           -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.quickSort(arr, cmp); }
            case "HeapSort"            -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.heapSort(arr, cmp); }
            case "BitonicSort"         -> {
                int p = nextPowerOfTwo(n);
                CleanedRecord[] padded = Arrays.copyOf(master, p);
                Arrays.fill(padded, n, p, buildSentinel());
                SortingAlgorithms.bitonicSort(padded, cmp);
                arr = Arrays.copyOf(padded, n);
            }
            case "GnomeSort"           -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.gnomeSort(arr, cmp); }
            case "BinaryInsertionSort" -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.binaryInsertionSort(arr, cmp); }
            case "RadixSort"           -> { arr = Arrays.copyOf(master, n); SortingAlgorithms.radixSort(arr, cmp, keyFn); }
            default -> throw new IllegalArgumentException("Algoritmo desconocido: " + algorithm);
        }

        List<CleanedRecordDto> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, arr.length); i++) {
            result.add(new CleanedRecordDto(arr[i]));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------------------

    private List<SortingResultDto> runRound(CleanedRecord[] master,
                                             Comparator<CleanedRecord> cmp,
                                             ToLongFunction<CleanedRecord> keyFn) {
        List<SortingResultDto> results = new ArrayList<>();
        int n = Math.min(master.length, DATASET_SIZE);

        // Centinela para Bitonic Sort: el máximo bajo el comparador dado
        CleanedRecord sentinel = buildSentinel();

        results.add(time("TimSort",             n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.timSort(a, cmp);
        }));

        results.add(time("CombSort",            n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.combSort(a, cmp);
        }));

        results.add(time("SelectionSort",       n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.selectionSort(a, cmp);
        }));

        results.add(time("TreeSort",            n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.treeSort(a, cmp);
        }));

        results.add(time("PigeonholeSort",      n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.pigeonholeSort(a, cmp, keyFn);
        }));

        results.add(time("BucketSort",          n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.bucketSort(a, cmp, keyFn);
        }));

        results.add(time("QuickSort",           n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.quickSort(a, cmp);
        }));

        results.add(time("HeapSort",            n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.heapSort(a, cmp);
        }));

        results.add(time("BitonicSort",         n, () -> {
            int p = nextPowerOfTwo(n);
            CleanedRecord[] padded = Arrays.copyOf(master, p);
            Arrays.fill(padded, n, p, sentinel);
            SortingAlgorithms.bitonicSort(padded, cmp);
            // el resultado queda en padded[0..n-1]
        }));

        results.add(time("GnomeSort",           n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.gnomeSort(a, cmp);
        }));

        results.add(time("BinaryInsertionSort", n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.binaryInsertionSort(a, cmp);
        }));

        results.add(time("RadixSort",           n, () -> {
            CleanedRecord[] a = Arrays.copyOf(master, n);
            SortingAlgorithms.radixSort(a, cmp, keyFn);
        }));

        return results;
    }

    private List<CleanedRecordDto> extractTop15(CleanedRecord[] master) {
        int n = master.length;
        CleanedRecord[] arr = Arrays.copyOf(master, n);
        SortingAlgorithms.heapSort(arr, VOLUME_CMP);   // ASC por volumen
        // Los 15 de mayor volumen quedan al final; se devuelven en orden ascendente
        List<CleanedRecordDto> top15 = new ArrayList<>();
        int start = Math.max(0, n - 15);
        for (int i = start; i < n; i++) {
            top15.add(new CleanedRecordDto(arr[i]));
        }
        return top15;
    }

    private SortingResultDto time(String name, int size, Runnable algo) {
        long t0 = System.nanoTime();
        algo.run();
        double seconds = (System.nanoTime() - t0) / 1_000_000_000.0;
        return new SortingResultDto(name, size, seconds);
    }

    private static int nextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        int p = Integer.highestOneBit(n - 1) << 1;
        return p > 0 ? p : Integer.MIN_VALUE >>> 1; // guarda contra overflow
    }

    /**
     * Construye un registro centinela que sea "el mayor" en cualquier comparador,
     * para usarlo como relleno en Bitonic Sort.
     */
    private static CleanedRecord buildSentinel() {
        CleanedRecord s = new CleanedRecord();
        // Sentinela máxima bajo cualquier comparador: fecha máxima, close máximo, volumen máximo
        s.setDate(LocalDate.of(9999, 12, 31));
        s.setClose(BigDecimal.valueOf(Double.MAX_VALUE));
        s.setVolume(Long.MAX_VALUE);
        s.setSymbol("__SENTINEL__");
        s.setOpen(BigDecimal.ZERO);
        s.setHigh(BigDecimal.ZERO);
        s.setLow(BigDecimal.ZERO);
        s.setTransformedAt(LocalDateTime.now());
        return s;
    }
}
