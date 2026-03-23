package com.financial.etl.sorting;

import com.financial.etl.entity.CleanedRecord;

import java.util.Comparator;

/**
 * Criterio 1: fecha ASC
 * Criterio 2: close ASC (desempate)
 */
public class ActivoComparator implements Comparator<CleanedRecord> {

    @Override
    public int compare(CleanedRecord a, CleanedRecord b) {
        int c = a.getDate().compareTo(b.getDate());
        if (c != 0) return c;
        return a.getClose().compareTo(b.getClose());
    }
}
