package com.financial.etl.repository;

import com.financial.etl.entity.FinancialSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialSeriesRepository extends JpaRepository<FinancialSeries, Long> {

    List<FinancialSeries> findBySymbol(String symbol);

    Optional<FinancialSeries> findFirstBySymbol(String symbol);

    List<FinancialSeries> findByBatchId(Long batchId);
}
