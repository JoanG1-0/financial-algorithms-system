package com.financial.etl.transform.cleaner;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import com.financial.etl.entity.PriceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OhlcConsistencyCheckerTest {

    private OhlcConsistencyChecker checker;

    @BeforeEach
    void setUp() {
        checker = new OhlcConsistencyChecker();
    }

    @Test
    void validate_cleanRecord_returnsClean() {
        PriceRecord pr = record("100", "110", "90", "105", 1000L);
        List<CleanedRecord> result = checker.validate("AAPL", List.of(pr));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDataQuality()).isEqualTo(DataQuality.CLEAN);
        assertThat(result.get(0).getAnomalyType()).isNull();
    }

    @Test
    void validate_lowGreaterThanHigh_swapsAndCorrects() {
        // low=110, high=90 → inverted
        PriceRecord pr = record("100", "90", "110", "95", 500L);
        List<CleanedRecord> result = checker.validate("AAPL", List.of(pr));

        CleanedRecord cr = result.get(0);
        assertThat(cr.getDataQuality()).isEqualTo(DataQuality.ANOMALY_CORRECTED);
        assertThat(cr.getAnomalyType()).isEqualTo(OhlcConsistencyChecker.OHLC_INCONSISTENCY);
        // After swap: low=90, high=110
        assertThat(cr.getLow()).isEqualByComparingTo("90");
        assertThat(cr.getHigh()).isEqualByComparingTo("110");
    }

    @Test
    void validate_zeroPriceOnClose_flagsZeroPrice() {
        PriceRecord pr = record("100", "110", "90", "0", 500L);
        List<CleanedRecord> result = checker.validate("AAPL", List.of(pr));

        assertThat(result.get(0).getDataQuality()).isEqualTo(DataQuality.ANOMALY_FLAGGED);
        assertThat(result.get(0).getAnomalyType()).isEqualTo(OhlcConsistencyChecker.ZERO_PRICE);
    }

    @Test
    void validate_negativePriceOnOpen_flagsZeroPrice() {
        PriceRecord pr = record("-1", "110", "90", "105", 500L);
        List<CleanedRecord> result = checker.validate("AAPL", List.of(pr));

        assertThat(result.get(0).getDataQuality()).isEqualTo(DataQuality.ANOMALY_FLAGGED);
        assertThat(result.get(0).getAnomalyType()).isEqualTo(OhlcConsistencyChecker.ZERO_PRICE);
    }

    @Test
    void validate_openAboveHigh_flagsOhlcInconsistency() {
        // open=120 > high=110
        PriceRecord pr = record("120", "110", "90", "105", 500L);
        List<CleanedRecord> result = checker.validate("AAPL", List.of(pr));

        assertThat(result.get(0).getDataQuality()).isEqualTo(DataQuality.ANOMALY_FLAGGED);
        assertThat(result.get(0).getAnomalyType()).isEqualTo(OhlcConsistencyChecker.OHLC_INCONSISTENCY);
    }

    @Test
    void validate_closeBelowLow_flagsOhlcInconsistency() {
        // close=80 < low=90
        PriceRecord pr = record("100", "110", "90", "80", 500L);
        List<CleanedRecord> result = checker.validate("AAPL", List.of(pr));

        assertThat(result.get(0).getDataQuality()).isEqualTo(DataQuality.ANOMALY_FLAGGED);
        assertThat(result.get(0).getAnomalyType()).isEqualTo(OhlcConsistencyChecker.OHLC_INCONSISTENCY);
    }

    @Test
    void validate_multipleRecords_processedIndependently() {
        PriceRecord clean = record("100", "110", "90", "105", 1000L);
        PriceRecord inverted = record("100", "90", "110", "95", 500L);

        List<CleanedRecord> result = checker.validate("AAPL", List.of(clean, inverted));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDataQuality()).isEqualTo(DataQuality.CLEAN);
        assertThat(result.get(1).getDataQuality()).isEqualTo(DataQuality.ANOMALY_CORRECTED);
    }

    @Test
    void validate_emptyList_returnsEmptyList() {
        List<CleanedRecord> result = checker.validate("AAPL", List.of());
        assertThat(result).isEmpty();
    }

    // Helper
    private PriceRecord record(String open, String high, String low, String close, long volume) {
        PriceRecord pr = new PriceRecord();
        pr.setDatetime(LocalDate.of(2024, 1, 2));
        pr.setOpen(new BigDecimal(open));
        pr.setHigh(new BigDecimal(high));
        pr.setLow(new BigDecimal(low));
        pr.setClose(new BigDecimal(close));
        pr.setVolume(volume);
        return pr;
    }
}
