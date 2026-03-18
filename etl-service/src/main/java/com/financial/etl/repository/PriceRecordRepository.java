package com.financial.etl.repository;

import com.financial.etl.entity.PriceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    Page<PriceRecord> findBySeriesSymbolOrderByDatetimeDesc(String symbol, Pageable pageable);
}
