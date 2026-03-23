package com.financial.etl.sorting;

import com.financial.etl.entity.CleanedRecord;

import java.util.Comparator;

/**
 * Criterio: volume DESC (mayor volumen primero).
 * Registros con volume null se consideran menores que cualquier valor real.
 */
public class VolumeComparator implements Comparator<CleanedRecord> {

    @Override
    public int compare(CleanedRecord a, CleanedRecord b) {
        long va = a.getVolume() != null ? a.getVolume() : Long.MIN_VALUE;
        long vb = b.getVolume() != null ? b.getVolume() : Long.MIN_VALUE;
        return Long.compare(vb, va); // DESC
    }
}
