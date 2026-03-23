package com.financial.etl.dto;

import java.util.List;

public class BenchmarkSummaryDto {

    private List<SortingResultDto> sortByDateClose;
    private List<SortingResultDto> sortByVolume;
    private List<CleanedRecordDto> top15ByVolume;

    public BenchmarkSummaryDto(List<SortingResultDto> sortByDateClose,
                                List<SortingResultDto> sortByVolume,
                                List<CleanedRecordDto> top15ByVolume) {
        this.sortByDateClose = sortByDateClose;
        this.sortByVolume    = sortByVolume;
        this.top15ByVolume   = top15ByVolume;
    }

    public List<SortingResultDto> getSortByDateClose() { return sortByDateClose; }
    public List<SortingResultDto> getSortByVolume()    { return sortByVolume; }
    public List<CleanedRecordDto> getTop15ByVolume()   { return top15ByVolume; }
}
