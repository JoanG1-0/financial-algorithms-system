package com.financial.etl.repository;

import com.financial.etl.entity.FinancialSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FinancialSeriesRepository extends JpaRepository<FinancialSeries, Long> {

    List<FinancialSeries> findBySymbol(String symbol);

    Optional<FinancialSeries> findFirstBySymbol(String symbol);

    List<FinancialSeries> findByBatchId(Long batchId);

    @Query("SELECT DISTINCT s FROM FinancialSeries s LEFT JOIN FETCH s.priceRecords")
    List<FinancialSeries> findAllWithPriceRecords();
}
