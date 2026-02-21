package com.financial.etl.repository;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Map;
import java.util.List;

public interface CleanedRecordRepository extends JpaRepository<CleanedRecord, Long> {

    @Query("SELECT c.dataQuality AS quality, COUNT(c) AS total FROM CleanedRecord c GROUP BY c.dataQuality")
    List<QualityCount> countByDataQuality();

    interface QualityCount {
        DataQuality getQuality();
        Long getTotal();
    }
}
