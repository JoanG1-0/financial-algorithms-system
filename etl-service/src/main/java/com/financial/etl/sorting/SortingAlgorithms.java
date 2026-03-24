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

    //Esto define el tamaño en el que se van a dividir los bloques que seran ordenados por Insertion Sort, bloques de tamaño 32
    private static final int RUN = 32; 

    /**
     * En este metodo de sorteo se entregan cinco parametros:
     * a = Arreglo original de activos
     * l = inicio
     * m = mitad
     * r = fin
     * cmp = comparador
     */
    private static void mergeTim(CleanedRecord[] a, int l, int m, int r,
                                  Comparator<CleanedRecord> cmp) {


    //Divide el arreglo en izquierda y derecha
        CleanedRecord[] left  = Arrays.copyOfRange(a, l, m + 1);
        CleanedRecord[] right = Arrays.copyOfRange(a, m + 1, r + 1);

    //Indices de recorrido i = izquierda, j = derecha y k = posicion en el arreglo original
        int i = 0;
        int j = 0;
        int k = l;

    /**
     * Este metodo luego de tener los bloques de 32 dividos empiezan a comparar valores entre si
     * y el menor lo va insertando en la lista de menor a mayor 
     * */
        while (i < left.length && j < right.length)
            a[k++] = cmp.compare(left[i], right[j]) <= 0 ? left[i++] : right[j++];
        while (i < left.length)  a[k++] = left[i++];
        while (j < right.length) a[k++] = right[j++];
    }

    /**
     * Este es el metodo encargado de tener cada bloque de 32 y organizarlo internamente, toma el primer valor
     * de cada sub-lista y va acomodandola nuevamente comparando que el valor siguiente sea el menor o mayor asi
     * dandole un orden establecido de menor a mayor a cada sub-lista para luego utilizar el metodo de mergeTim
     */
    public static void timSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        int n = a.length;
        for (int i = 0; i < n; i += RUN)
            insertionSortRange(a, i, Math.min(i + RUN - 1, n - 1), cmp); //Metodo InsertionSort
        for (int size = RUN; size < n; size *= 2) // size es el tamaño de los bloques a mergear, empieza en 32 y se va duplicando
            for (int left = 0; left < n; left += 2 * size) { // left es el inicio del bloque izquierdo, se va incrementando en 64 cada iteracion
                int mid   = Math.min(left + size - 1, n - 1); // mid es el final del bloque izquierdo, se va incrementando en 32 cada iteracion
                int right = Math.min(left + 2 * size - 1, n - 1);// right es el final del bloque derecho, se va incrementando en 64 cada iteracion
                if (mid < right) mergeTim(a, left, mid, right, cmp); // Si el bloque izquierdo no esta vacio, se llama a mergeTim para ordenar los bloques izquierdo y derecho
            }
    }

    // -------------------------------------------------------------------------
    // 2 — Comb Sort · O(n log n) promedio
    // -------------------------------------------------------------------------

    public static void combSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        int n = a.length;
        int gap = n; // gap es la distancia entre los elementos a comparar, empieza siendo el tamaño del arreglo y se va reduciendo en cada iteracion
        boolean sorted = false; // sorted indica si el arreglo esta ordenado, empieza siendo false y se vuelve true cuando el gap es 1 y no se hicieron swaps
        while (!sorted) {
            gap = (int) (gap / 1.3); // Se reduce el gap dividiendolo por 1.3, este es un factor empirico que se ha demostrado que funciona bien para Comb Sort
            if (gap <= 1) { gap = 1; sorted = true; }
            for (int i = 0; i + gap < n; i++)
                if (cmp.compare(a[i], a[i + gap]) > 0) {// Si el elemento en la posicion i es mayor que el elemento en la posicion i + gap, se hace un swap
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
        for (int i = 0; i < n - 1; i++) {// Para cada posicion i, se busca el minimo elemento en el rango [i+1, n-1] y se hace un swap con el elemento en la posicion i
            int min = i;
            for (int j = i + 1; j < n; j++)// Si el elemento en la posicion j es menor que el elemento en la posicion min, se actualiza min
                if (cmp.compare(a[j], a[min]) < 0) min = j;
            swap(a, i, min);
        }
    }

    // -------------------------------------------------------------------------
    // 4 — Tree Sort · O(n log n) promedio — completamente iterativo
    // -------------------------------------------------------------------------

    private static final class BSTNode { // Nodo de un árbol binario de búsqueda
        CleanedRecord val; // Valor almacenado en el nodo
        BSTNode left; // Hijo izquierdo
        BSTNode right; // Hijo derecho
        BSTNode(CleanedRecord v) { 
            val = v; 
        } // Constructor que inicializa el valor del nodo
    }

    private static BSTNode bstInsert(BSTNode root, CleanedRecord val,
                                      Comparator<CleanedRecord> cmp) {
        if (root == null) return new BSTNode(val);// Si el árbol está vacío, se crea un nuevo nodo con el valor dado y se devuelve como raíz
        BSTNode curr = root;//Se inicia la inserción desde la raíz del árbol
        boolean inserted = false;// Variable para controlar cuándo se ha insertado el nuevo nodo
        while (!inserted) {
            if (cmp.compare(val, curr.val) < 0) {

                /**
                 * // Si el valor a insertar es menor que el valor del nodo actual y el hijo izquierdo es nulo, 
                 * se crea un nuevo nodo con el valor dado y se asigna como hijo izquierdo del nodo actual. Luego se marca como insertado.
                 */
                if (curr.left == null) { curr.left  = new BSTNode(val); inserted = true; }
                else curr = curr.left;
            } else {

                /**
                 * // Si el valor a insertar es mayor o igual que el valor del nodo actual y el hijo derecho es nulo, 
                 * se crea un nuevo nodo con el valor dado y se asigna como hijo derecho del nodo actual. Luego se marca como insertado.
                 */
                if (curr.right == null) { curr.right = new BSTNode(val); inserted = true; }
                else curr = curr.right;
            }
        }
        return root;
    }

    private static void inorder(BSTNode root, CleanedRecord[] a, int[] idx) {
        Deque<BSTNode> stack = new ArrayDeque<>();// Stack para realizar el recorrido inorden iterativo
        BSTNode curr = root;
        while (curr != null || !stack.isEmpty()) {
            while (curr != null) { stack.push(curr); curr = curr.left; }// Se recorren los nodos hacia la izquierda y se van apilando en el stack
            curr = stack.pop();// Se visita el nodo actual, se asigna su valor al arreglo en la posición indicada por idx[0] y se incrementa idx[0]
            a[idx[0]++] = curr.val;// Luego se procede a recorrer el subárbol derecho del nodo actual
            curr = curr.right;// Se repite el proceso hasta que se hayan visitado todos los nodos del árbol
        }
    }

    public static void treeSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        BSTNode root = null;
        for (CleanedRecord r : a) root = bstInsert(root, r, cmp);// Se construye el árbol binario de búsqueda insertando cada elemento del arreglo
        inorder(root, a, new int[]{0});// Se realiza un recorrido inorden del árbol para llenar el arreglo con los elementos ordenados
    }

    // -------------------------------------------------------------------------
    // 5 — Pigeonhole Sort · O(n + rango)
    //     keyFn: extractor numérico (p.ej. date.toEpochDay() o volume)
    //     Se limita a MAX_HOLES casilleros para evitar OOM con rangos grandes.
    // -------------------------------------------------------------------------

    private static final int MAX_HOLES = 10_000;// Límite de casilleros para Pigeonhole Sort

    public static void pigeonholeSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp,
                                      ToLongFunction<CleanedRecord> keyFn) {
        if (a.length == 0) return;
        long min = Long.MAX_VALUE;// Se encuentra el valor mínimo y máximo de las claves para determinar el rango
        long max = Long.MIN_VALUE;// Si el rango es mayor que MAX_HOLES, se limita a MAX_HOLES para evitar problemas de memoria
        for (CleanedRecord r : a) {
            long k = keyFn.applyAsLong(r);// Se aplica la función de clave a cada elemento para obtener su valor numérico
            if (k < min) min = k;// Se actualiza el valor mínimo si la clave actual es menor que el mínimo registrado
            if (k > max) max = k;// Se actualiza el valor máximo si la clave actual es mayor que el máximo registrado
        }
        long range = max - min + 1;// Se calcula el rango de las claves
        int holes = (int) Math.min(range, MAX_HOLES);// Se determina el número de casilleros a utilizar, limitándolo a MAX_HOLES
        @SuppressWarnings("unchecked")
        List<CleanedRecord>[] holeArr = new List[holes];// Se crea un arreglo de listas para almacenar los elementos en sus respectivos casilleros
        for (int i = 0; i < holes; i++) holeArr[i] = new ArrayList<>();
        for (CleanedRecord r : a) {

            // Se calcula el índice del casillero para cada elemento utilizando su clave y el rango, asegurándose de no exceder el número de casilleros
            int idx = (int) Math.min((long)holes - 1, ((keyFn.applyAsLong(r) - min) * holes) / range);
            holeArr[idx].add(r);// Se agrega el elemento al casillero correspondiente
        }
        int k = 0;
        for (List<CleanedRecord> hole : holeArr) {
            insertionSortList(hole, cmp);// Se ordena cada casillero utilizando Insertion Sort para mantener el orden dentro de cada casillero
            for (CleanedRecord r : hole) a[k++] = r;// Se copian los elementos ordenados de cada casillero de vuelta al arreglo original
        }
    }

    // -------------------------------------------------------------------------
    // 6 — Bucket Sort · O(n + k),  k = √n cubetas por rango de clave
    // -------------------------------------------------------------------------

    public static void bucketSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp,
                                  ToLongFunction<CleanedRecord> keyFn) {
        if (a.length == 0) return;
        long min = Long.MAX_VALUE; // Se encuentra el valor mínimo y máximo de las claves para determinar el rango
        long max = Long.MIN_VALUE;// Si el rango es mayor que MAX_HOLES, se limita a MAX_HOLES para evitar problemas de memoria
        for (CleanedRecord r : a) {

            //Lo mismo que en Pigeonhole Sort, se aplica la función de clave a cada elemento para obtener su valor numérico y se actualizan los valores mínimo y máximo
            long k = keyFn.applyAsLong(r);
            if (k < min) min = k;
            if (k > max) max = k;
        }
        // Se determina el número de cubetas a utilizar, calculándolo como la raíz cuadrada del tamaño del arreglo para mantener un equilibrio entre el número de elementos por cubeta
        int bc = Math.max(1, (int) Math.sqrt(a.length));
        long rng = Math.max(1, max - min + 1);// Se calcula el rango de las claves para distribuir los elementos en las cubetas de manera uniforme
        @SuppressWarnings("unchecked")
        List<CleanedRecord>[] buckets = new List[bc];
        for (int i = 0; i < bc; i++) buckets[i] = new ArrayList<>();// Se crea un arreglo de listas para representar las cubetas
        for (CleanedRecord r : a) {

            //igual que en Pigeonhole Sort, se calcula el índice de la cubeta para cada elemento utilizando su clave y el rango, asegurándose de no exceder el número de cubetas
            int idx = (int) Math.min((long)bc - 1, ((keyFn.applyAsLong(r) - min) * bc) / rng);
            buckets[idx].add(r);
        }
        int k = 0;
        for (List<CleanedRecord> b : buckets) {
            insertionSortList(b, cmp);// Se ordena cada cubeta utilizando Insertion Sort para mantener el orden dentro de cada cubeta
            for (CleanedRecord r : b) a[k++] = r;// Se copian los elementos ordenados de cada cubeta de vuelta al arreglo original
        }
    }

    // -------------------------------------------------------------------------
    // 7 — QuickSort · O(n log n) promedio — pila explícita, sin recursión
    // -------------------------------------------------------------------------


    /**
     * Este método es el encargado de realizar la partición del arreglo para el algoritmo de QuickSort.
     * lo = indice de inicio del subarreglo a particionar
     * hi = indice de fin del subarreglo a particionar
     */
    private static int partition(CleanedRecord[] a, int lo, int hi,
                                  Comparator<CleanedRecord> cmp) {
        int mid = lo + (hi - lo) / 2;// Se elige el pivote utilizando la mediana de tres para mejorar el rendimiento en casos ya ordenados o casi ordenados

        // Se ordenan los elementos en las posiciones lo, mid y hi para asegurar que el pivote esté en la posición correcta después de la partición
        if (cmp.compare(a[lo],  a[mid]) > 0) swap(a, lo, mid);
        if (cmp.compare(a[lo],  a[hi])  > 0) swap(a, lo, hi);
        if (cmp.compare(a[mid], a[hi])  > 0) swap(a, mid, hi);

        swap(a, mid, hi - 1);// Se mueve el pivote a la posición hi - 1 para facilitar la partición
        CleanedRecord pivot = a[hi - 1];// Se inicia la partición utilizando dos índices i y j que recorren el subarreglo desde ambos extremos hacia el centro
        int i = lo;
        int j = hi - 1;

        /**
         * El índice i se mueve hacia la derecha mientras los elementos sean menores que el pivote, y el índice j se 
         * mueve hacia la izquierda mientras los elementos sean mayores que el pivote.
         */
        while (true) {
            while (cmp.compare(a[++i], pivot) < 0);// Se incrementa i mientras el elemento en la posición i sea menor que el pivote
            while (cmp.compare(a[--j], pivot) > 0);// Se decrementa j mientras el elemento en la posición j sea mayor que el pivote
            if (i >= j) break;// Cuando los índices se cruzan, se detiene la partición
            swap(a, i, j);// Se hace un swap de los elementos en las posiciones i y j para colocar los elementos menores que el pivote a la izquierda y los mayores a la derecha
        }
        swap(a, i, hi - 1);// Se coloca el pivote en su posición final intercambiándolo con el elemento en la posición i
        return i;// Se devuelve el índice del pivote después de la partición, que se utilizará para dividir el arreglo en subarreglos para las siguientes iteraciones de QuickSort
    }

    public static void quickSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        if (a.length < 2) return;// Si el arreglo tiene menos de 2 elementos, ya está ordenado
        // Se utiliza una pila explícita para simular la recursión de QuickSort, almacenando los índices de los subarreglos que aún necesitan ser ordenados
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, a.length - 1});// Se inicia la pila con el rango completo del arreglo
        while (!stack.isEmpty()) {
            int[] r = stack.pop();// Se obtiene el rango del subarreglo a ordenar
            int lo = r[0];// Se extraen los índices de inicio y fin del subarreglo
            int hi = r[1];// Si el tamaño del subarreglo es menor que 10, se utiliza Insertion Sort para ordenar ese rango, ya que es más eficiente para arreglos pequeños
            if (hi - lo < 10) {
                insertionSortRange(a, lo, hi, cmp);// Si el subarreglo es lo suficientemente pequeño, se ordena directamente con Insertion Sort
            } else if (lo < hi) {
                int p = partition(a, lo, hi, cmp);// Se particiona el subarreglo y se obtiene el índice del pivote
                stack.push(new int[]{lo, p - 1});// Se agrega el rango del subarreglo izquierdo a la pila para ser ordenado en futuras iteraciones
                stack.push(new int[]{p + 1, hi});// Se agrega el rango del subarreglo derecho a la pila para ser ordenado en futuras iteraciones
            }
        }
    }

    // -------------------------------------------------------------------------
    // 8 — HeapSort · O(n log n)
    // -------------------------------------------------------------------------


    /**
     * Este método es el encargado de mantener la propiedad de heap en un subarreglo del arreglo 
     * original, partiendo desde el índice i y considerando un tamaño n.
     */
    private static void heapify(CleanedRecord[] a, int n, int i,
                                 Comparator<CleanedRecord> cmp) {
        while (true) {
            int largest = i; // Se asume que el nodo actual es el más grande
            int l = 2 * i + 1; // Se calcula el índice del hijo izquierdo
            int r = 2 * i + 2; // Se calcula el índice del hijo derecho
            if (l < n && cmp.compare(a[l], a[largest]) > 0) largest = l; // Si el hijo izquierdo es mayor que el nodo actual, se actualiza largest
            if (r < n && cmp.compare(a[r], a[largest]) > 0) largest = r; // Si el hijo derecho es mayor que el nodo actual, se actualiza largest
            if (largest == i) break; // Si el nodo actual es el más grande, se detiene el proceso
            swap(a, i, largest); // Se hace un swap entre el nodo actual y el nodo más grande para mantener la propiedad de heap
            i = largest; // Se actualiza el índice i al nuevo nodo que se ha movido para continuar verificando la propiedad de heap en el subárbol afectado por el swap
        }
    }

    public static void heapSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        int n = a.length; // Se construye el heap a partir del arreglo, comenzando desde el último nodo no hoja y aplicando heapify hacia arriba
        // Se extraen los elementos del heap uno por uno, colocando el elemento más grande al final del arreglo y luego aplicando heapify para restaurar la propiedad de heap en el subarreglo restante
        for (int i = n / 2 - 1; i >= 0; i--) heapify(a, n, i, cmp); 
        for (int i = n - 1; i > 0; i--) { // Se mueve el elemento más grande (raíz del heap) al final del arreglo y se reduce el tamaño del heap en 1
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
        if (asc == (cmp.compare(a[i], a[j]) > 0)) swap(a, i, j); // Si asc es true y a[i] > a[j], o si asc es false y a[i] < a[j], se hace un swap para ordenar en el orden correcto
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
            if (i == 0 || cmp.compare(a[i], a[i - 1]) >= 0) i++; // Si el elemento actual es mayor o igual que el anterior, se avanza al siguiente elemento
            else { swap(a, i, i - 1); i--; }// Si el elemento actual es menor que el anterior, se hace un swap para corregir el orden y se retrocede para comparar con el nuevo elemento anterior
        }
    }

    // -------------------------------------------------------------------------
    // 11 — Binary Insertion Sort · O(n²)  ⚠ usar subconjunto 3 000
    // -------------------------------------------------------------------------

    private static int binarySearch(CleanedRecord[] a, CleanedRecord val,
                                     int lo, int hi, Comparator<CleanedRecord> cmp) {
        while (lo <= hi) {
            int mid = (lo + hi) / 2; // Se calcula el índice medio del rango de búsqueda
            // Si el elemento en la posición media es menor que el valor buscado, se ajusta el límite inferior de la búsqueda para continuar buscando en la mitad superior del rango
            if (cmp.compare(a[mid], val) < 0) lo = mid + 1; 
            else hi = mid - 1;// Si el elemento en la posición media es mayor o igual que el valor buscado, se ajusta el límite superior de la búsqueda para continuar buscando en la mitad inferior del rango
        }
        return lo;
    }

    public static void binaryInsertionSort(CleanedRecord[] a, Comparator<CleanedRecord> cmp) {
        for (int i = 1; i < a.length; i++) { // Para cada elemento a partir del segundo, se realiza una búsqueda binaria en el subarreglo ordenado a la izquierda para encontrar la posición correcta del elemento actual
            CleanedRecord val = a[i];// Se guarda el valor actual para insertarlo en la posición correcta después de desplazar los elementos mayores
            int pos = binarySearch(a, val, 0, i - 1, cmp); // Se realiza una búsqueda binaria para encontrar la posición correcta del elemento actual en el subarreglo ordenado a la izquierda
            System.arraycopy(a, pos, a, pos + 1, i - pos); // Se desplazan los elementos mayores que el valor actual hacia la derecha para hacer espacio para la inserción
            a[pos] = val; // Se inserta el valor actual en la posición correcta encontrada por la búsqueda binaria
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

        // Se cuenta la frecuencia de cada dígito (0-9) en la posición actual (determinado por exp) para cada elemento del arreglo utilizando la función de clave keyFn para extraer el valor numérico y calcular el dígito correspondiente
        int[] count = new int[10]; 
        for (CleanedRecord r : a) count[(int) ((keyFn.applyAsLong(r) / exp) % 10)]++; // Se incrementa el contador para el dígito correspondiente al elemento actual
        for (int i = 1; i < 10; i++) count[i] += count[i - 1]; // Se acumulan los contadores para obtener las posiciones finales de cada dígito en el arreglo de salida
        for (int i = n - 1; i >= 0; i--) { // Se construye el arreglo de salida colocando los elementos en la posición correcta según el dígito actual, iterando desde el final del arreglo para mantener la estabilidad del ordenamiento
            int d = (int) ((keyFn.applyAsLong(a[i]) / exp) % 10);
            output[--count[d]] = a[i];
        }
        System.arraycopy(output, 0, a, 0, n); // Se copia el arreglo de salida de vuelta al arreglo original para que esté ordenado según el dígito actual
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
