package com.financial.etl.repository;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CleanedRecordRepository extends JpaRepository<CleanedRecord, Long> {

    @Query("SELECT c.dataQuality AS quality, COUNT(c) AS total FROM CleanedRecord c GROUP BY c.dataQuality")
    List<QualityCount> countByDataQuality();

    @Query("SELECT c FROM CleanedRecord c WHERE c.dataQuality IN (:qualities) ORDER BY c.symbol ASC, c.date ASC")
    List<CleanedRecord> findByDataQualityInOrderBySymbolAndDate(
            @org.springframework.data.repository.query.Param("qualities") List<DataQuality> qualities);

    Page<CleanedRecord> findBySymbolOrderByDateDesc(String symbol, Pageable pageable);

    @Query("SELECT c.symbol AS symbol, COUNT(c) AS total FROM CleanedRecord c " +
           "WHERE c.dataQuality = com.financial.etl.entity.DataQuality.FORWARD_FILLED " +
           "GROUP BY c.symbol ORDER BY c.symbol ASC")
    List<SymbolMissingCount> countForwardFilledPerSymbol();

    interface QualityCount {
        DataQuality getQuality();
        Long getTotal();
    }

    interface SymbolMissingCount {
        String getSymbol();
        Long getTotal();
    }
}
