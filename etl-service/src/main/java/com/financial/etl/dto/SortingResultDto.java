package com.financial.etl.dto;

public class SortingResultDto {

    private String algorithm;
    private int size;
    private double timeSeconds;

    public SortingResultDto(String algorithm, int size, double timeSeconds) {
        this.algorithm   = algorithm;
        this.size        = size;
        this.timeSeconds = timeSeconds;
    }

    public String getAlgorithm()   { return algorithm; }
    public int    getSize()        { return size; }
    public double getTimeSeconds() { return timeSeconds; }
}
