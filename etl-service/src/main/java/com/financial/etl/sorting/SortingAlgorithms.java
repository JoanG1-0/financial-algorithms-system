package com.financial.etl.sorting;

import com.financial.etl.entity.CleanedRecord;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * 12 algoritmos de ordenamiento implementados desde cero sobre CleanedRecord[].
 *
 * Todos los métodos de comparación aceptan un Comparator<CleanedRecord>.
 * Los algoritmos no-comparativos (Pigeonhole, Bucket, Radix) también aceptan
 * un ToLongFunction<CleanedRecord> como extractor de clave numérica.
 *
 * Restricciones cumplidas:
 * - Sin Arrays.sort() ni Collections.sort()
 * - TreeSort y QuickSort son iterativos (sin recursión)
 * - Bitonic Sort usa recursión segura (profundidad O(log n))
 */
public final class SortingAlgorithms {

    private SortingAlgorithms() {}

    // -------------------------------------------------------------------------
    // Utilidades privadas
    // -------------------------------------------------------------------------

    private static void swap(CleanedRecord[] a, int i, int j) {
        CleanedRecord tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    private static void insertionSortRange(CleanedRecord[] a, int left, int right,
                                           Comparator<CleanedRecord> cmp) {
        for (int i = left + 1; i <= right; i++) {
            CleanedRecord tmp = a[i];
            int j = i - 1;
            while (j >= left && cmp.compare(a[j], tmp) > 0) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = tmp;
        }
    }

    private static void insertionSortList(List<CleanedRecord> list, Comparator<CleanedRecord> cmp) {
        for (int i = 1; i < list.size(); i++) {
            CleanedRecord tmp = list.get(i);
            int j = i - 1;
            while (j >= 0 && cmp.compare(list.get(j), tmp) > 0) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, tmp);
        }
    }

    // -------------------------------------------------------------------------
    // 1 — TimSort · O(n log n)
    // -------------------------------------------------------------------------

    private static final int RUN = 32;

    private static void mergeTim(CleanedRecord[] a, int l, int m, int r,
                                  Comparator<CleanedRecord> cmp) {
        CleanedRecord[] left  = Arrays.copyOfRange(a, l, m + 1);
        CleanedRecord[] right = Arrays.copyOfRange(a, m + 1, r + 1);
        int i = 0;
        int j = 0;
        int k = l;
        while (i < left.length && j < right.length)
            a[k++] = cmp.compare(left[i], right[j]) <= 0 ? left[i++] : right[j++];
        while (i < left.length)  a[k++] = left[i++];
        while (j < right.length) a[k++] = right[j++];
    }

    public static void timSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        int n = a.length;
        for (int i = 0; i < n; i += RUN)
            insertionSortRange(a, i, Math.min(i + RUN - 1, n - 1), cmp);
        for (int size = RUN; size < n; size *= 2)
            for (int left = 0; left < n; left += 2 * size) {
                int mid   = Math.min(left + size - 1, n - 1);
                int right = Math.min(left + 2 * size - 1, n - 1);
                if (mid < right) mergeTim(a, left, mid, right, cmp);
            }
    }

    // -------------------------------------------------------------------------
    // 2 — Comb Sort · O(n log n) promedio
    // -------------------------------------------------------------------------

    public static void combSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        int n = a.length;
        int gap = n;
        boolean sorted = false;
        while (!sorted) {
            gap = (int) (gap / 1.3);
            if (gap <= 1) { gap = 1; sorted = true; }
            for (int i = 0; i + gap < n; i++)
                if (cmp.compare(a[i], a[i + gap]) > 0) {
                    swap(a, i, i + gap);
                    sorted = false;
                }
        }
    }

    // -------------------------------------------------------------------------
    // 3 — Selection Sort · O(n²)  ⚠ usar subconjunto 3 000
    // -------------------------------------------------------------------------

    public static void selectionSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            int min = i;
            for (int j = i + 1; j < n; j++)
                if (cmp.compare(a[j], a[min]) < 0) min = j;
            swap(a, i, min);
        }
    }

    // -------------------------------------------------------------------------
    // 4 — Tree Sort · O(n log n) promedio — completamente iterativo
    // -------------------------------------------------------------------------

    private static final class BSTNode {
        CleanedRecord val;
        BSTNode left;
        BTSNode right;
        BSTNode(CleanedRecord v) { val = v; }
    }

    private static BSTNode bstInsert(BSTNode root, CleanedRecord val,
                                      Comparator<CleanedRecord> cmp) {
        if (root == null) return new BSTNode(val);
        BSTNode curr = root;
        boolean inserted = false;
        while (!inserted) {
            if (cmp.compare(val, curr.val) < 0) {
                if (curr.left == null) { curr.left  = new BSTNode(val); inserted = true; }
                else curr = curr.left;
            } else {
                if (curr.right == null) { curr.right = new BSTNode(val); inserted = true; }
                else curr = curr.right;
            }
        }
        return root;
    }

    private static void inorder(BSTNode root, CleanedRecord[] a, int[] idx) {
        Deque<BSTNode> stack = new ArrayDeque<>();
        BSTNode curr = root;
        while (curr != null || !stack.isEmpty()) {
            while (curr != null) { stack.push(curr); curr = curr.left; }
            curr = stack.pop();
            a[idx[0]++] = curr.val;
            curr = curr.right;
        }
    }

    public static void treeSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        BSTNode root = null;
        for (CleanedRecord r : a) root = bstInsert(root, r, cmp);
        inorder(root, a, new int[]{0});
    }

    // -------------------------------------------------------------------------
    // 5 — Pigeonhole Sort · O(n + rango)
    //     keyFn: extractor numérico (p.ej. date.toEpochDay() o volume)
    //     Se limita a MAX_HOLES casilleros para evitar OOM con rangos grandes.
    // -------------------------------------------------------------------------

    private static final int MAX_HOLES = 10_000;

    public static void pigeonholeSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp,
                                      ToLongFunction<CleanedRecord> keyFn) {
        if (a.length == 0) return;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (CleanedRecord r : a) {
            long k = keyFn.applyAsLong(r);
            if (k < min) min = k;
            if (k > max) max = k;
        }
        long range = max - min + 1;
        int holes = (int) Math.min(range, MAX_HOLES);
        @SuppressWarnings("unchecked")
        List<CleanedRecord>[] holeArr = new List[holes];
        for (int i = 0; i < holes; i++) holeArr[i] = new ArrayList<>();
        for (CleanedRecord r : a) {
            int idx = (int) Math.min((long)holes - 1, ((keyFn.applyAsLong(r) - min) * holes) / range);
            holeArr[idx].add(r);
        }
        int k = 0;
        for (List<CleanedRecord> hole : holeArr) {
            insertionSortList(hole, cmp);
            for (CleanedRecord r : hole) a[k++] = r;
        }
    }

    // -------------------------------------------------------------------------
    // 6 — Bucket Sort · O(n + k),  k = √n cubetas por rango de clave
    // -------------------------------------------------------------------------

    public static void bucketSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp,
                                  ToLongFunction<CleanedRecord> keyFn) {
        if (a.length == 0) return;
        long min = Long.MAX_VALUE; 
        long max = Long.MIN_VALUE;
        for (CleanedRecord r : a) {
            long k = keyFn.applyAsLong(r);
            if (k < min) min = k;
            if (k > max) max = k;
        }
        int bc = Math.max(1, (int) Math.sqrt(a.length));
        long rng = Math.max(1, max - min + 1);
        @SuppressWarnings("unchecked")
        List<CleanedRecord>[] buckets = new List[bc];
        for (int i = 0; i < bc; i++) buckets[i] = new ArrayList<>();
        for (CleanedRecord r : a) {
            int idx = (int) Math.min((long)bc - 1, ((keyFn.applyAsLong(r) - min) * bc) / rng);
            buckets[idx].add(r);
        }
        int k = 0;
        for (List<CleanedRecord> b : buckets) {
            insertionSortList(b, cmp);
            for (CleanedRecord r : b) a[k++] = r;
        }
    }

    // -------------------------------------------------------------------------
    // 7 — QuickSort · O(n log n) promedio — pila explícita, sin recursión
    // -------------------------------------------------------------------------

    private static int partition(CleanedRecord[] a, int lo, int hi,
                                  Comparator<CleanedRecord> cmp) {
        int mid = lo + (hi - lo) / 2;
        if (cmp.compare(a[lo],  a[mid]) > 0) swap(a, lo, mid);
        if (cmp.compare(a[lo],  a[hi])  > 0) swap(a, lo, hi);
        if (cmp.compare(a[mid], a[hi])  > 0) swap(a, mid, hi);
        swap(a, mid, hi - 1);
        CleanedRecord pivot = a[hi - 1];
        int i = lo;
        int j = hi - 1;
        while (true) {
            while (cmp.compare(a[++i], pivot) < 0);
            while (cmp.compare(a[--j], pivot) > 0);
            if (i >= j) break;
            swap(a, i, j);
        }
        swap(a, i, hi - 1);
        return i;
    }

    public static void quickSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        if (a.length < 2) return;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, a.length - 1});
        while (!stack.isEmpty()) {
            int[] r = stack.pop();
            int lo = r[0];
            int hi = r[1];
            if (hi - lo < 10) {
                insertionSortRange(a, lo, hi, cmp);
            } else if (lo < hi) {
                int p = partition(a, lo, hi, cmp);
                stack.push(new int[]{lo, p - 1});
                stack.push(new int[]{p + 1, hi});
            }
        }
    }

    // -------------------------------------------------------------------------
    // 8 — HeapSort · O(n log n)
    // -------------------------------------------------------------------------

    private static void heapify(CleanedRecord[] a, int n, int i,
                                 Comparator<CleanedRecord> cmp) {
        while (true) {
            int largest = i;
            int l = 2 * i + 1; 
            int r = 2 * i + 2;
            if (l < n && cmp.compare(a[l], a[largest]) > 0) largest = l;
            if (r < n && cmp.compare(a[r], a[largest]) > 0) largest = r;
            if (largest == i) break;
            swap(a, i, largest);
            i = largest;
        }
    }

    public static void heapSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        int n = a.length;
        for (int i = n / 2 - 1; i >= 0; i--) heapify(a, n, i, cmp);
        for (int i = n - 1; i > 0; i--) {
            swap(a, 0, i);
            heapify(a, i, 0, cmp);
        }
    }

    // -------------------------------------------------------------------------
    // 9 — Bitonic Sort · O(n log² n)
    //     El arreglo DEBE ser potencia de 2. El llamador rellena con sentinelas.
    // -------------------------------------------------------------------------

    private static void bitonicCompare(CleanedRecord[] a, int i, int j,
                                        boolean asc, Comparator<CleanedRecord> cmp) {
        if (asc == (cmp.compare(a[i], a[j]) > 0)) swap(a, i, j);
    }

    private static void bitonicMerge(CleanedRecord[] a, int lo, int cnt,
                                      boolean asc, Comparator<CleanedRecord> cmp) {
        if (cnt > 1) {
            int k = cnt / 2;
            for (int i = lo; i < lo + k; i++) bitonicCompare(a, i, i + k, asc, cmp);
            bitonicMerge(a, lo, k, asc, cmp);
            bitonicMerge(a, lo + k, k, asc, cmp);
        }
    }

    private static void bitonicRec(CleanedRecord[] a, int lo, int cnt,
                                    boolean asc, Comparator<CleanedRecord> cmp) {
        if (cnt > 1) {
            int k = cnt / 2;
            bitonicRec(a, lo,     k, true,  cmp);
            bitonicRec(a, lo + k, k, false, cmp);
            bitonicMerge(a, lo, cnt, asc, cmp);
        }
    }

    /**
     * Ordena {@code a} usando Bitonic Sort.
     * El arreglo ya debe tener tamaño potencia de 2 (relleno con sentinelas por el llamador).
     */
    public static void bitonicSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        bitonicRec(a, 0, a.length, true, cmp);
    }

    // -------------------------------------------------------------------------
    // 10 — Gnome Sort · O(n²)  ⚠ usar subconjunto 3 000
    // -------------------------------------------------------------------------

    public static void gnomeSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        int i = 0;
        int n = a.length;
        while (i < n) {
            if (i == 0 || cmp.compare(a[i], a[i - 1]) >= 0) i++;
            else { swap(a, i, i - 1); i--; }
        }
    }

    // -------------------------------------------------------------------------
    // 11 — Binary Insertion Sort · O(n²)  ⚠ usar subconjunto 3 000
    // -------------------------------------------------------------------------

    private static int binarySearch(CleanedRecord[] a, CleanedRecord val,
                                     int lo, int hi, Comparator<CleanedRecord> cmp) {
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (cmp.compare(a[mid], val) < 0) lo = mid + 1;
            else hi = mid - 1;
        }
        return lo;
    }

    public static void binaryInsertionSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        for (int i = 1; i < a.length; i++) {
            CleanedRecord val = a[i];
            int pos = binarySearch(a, val, 0, i - 1, cmp);
            System.arraycopy(a, pos, a, pos + 1, i - pos);
            a[pos] = val;
        }
    }

    // -------------------------------------------------------------------------
    // 12 — Radix Sort · O(n · k)
    //      Counting Sort estable por cada dígito de keyFn.
    //      Dentro de grupos con la misma clave ordena por cmp (Insertion Sort).
    // -------------------------------------------------------------------------

    private static void countingSortByDigit(CleanedRecord[] a, long exp,
                                             ToLongFunction<CleanedRecord> keyFn) {
        int n = a.length;
        CleanedRecord[] output = new CleanedRecord[n];
        int[] count = new int[10];
        for (CleanedRecord r : a) count[(int) ((keyFn.applyAsLong(r) / exp) % 10)]++;
        for (int i = 1; i < 10; i++) count[i] += count[i - 1];
        for (int i = n - 1; i >= 0; i--) {
            int d = (int) ((keyFn.applyAsLong(a[i]) / exp) % 10);
            output[--count[d]] = a[i];
        }
        System.arraycopy(output, 0, a, 0, n);
    }

    public static void radixSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp,
                                  ToLongFunction<CleanedRecord> keyFn) {
        if (a.length == 0) return;
        long maxKey = 0;
        for (CleanedRecord r : a) {
            long k = keyFn.applyAsLong(r);
            if (k > maxKey) maxKey = k;
        }
        for (long exp = 1; maxKey / exp > 0; exp *= 10)
            countingSortByDigit(a, exp, keyFn);
        // Ordenar por cmp dentro de cada bloque de misma clave
        int i = 0;
        while (i < a.length) {
            int j = i + 1;
            long ki = keyFn.applyAsLong(a[i]);
            while (j < a.length && keyFn.applyAsLong(a[j]) == ki) j++;
            insertionSortRange(a, i, j - 1, cmp);
            i = j;
        }
    }
}
