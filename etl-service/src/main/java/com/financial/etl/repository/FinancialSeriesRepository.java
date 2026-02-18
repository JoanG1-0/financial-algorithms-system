package com.financial.etl.repository;

import com.financial.etl.entity.FinancialSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialSeriesRepository extends JpaRepository<FinancialSeries, Long> {

    List<FinancialSeries> findBySymbol(String symbol);
}
