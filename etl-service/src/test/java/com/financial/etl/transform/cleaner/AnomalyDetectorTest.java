package com.financial.etl.transform.cleaner;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyDetectorTest {

    private AnomalyDetector detector;

    @BeforeEach
    void setUp() {
        detector = new AnomalyDetector();
    }

    @Test
    void detectExtremeReturns_normalReturns_allRemainClean() {
        // Prices with tiny daily changes → z-scores well within 3σ
        List<CleanedRecord> records = buildRecords(
                new double[]{100, 100.5, 101, 100.8, 101.2, 100.9, 101.5}
        );

        detector.detectExtremeReturns(records);

        assertThat(records).allMatch(r -> r.getDataQuality() == DataQuality.CLEAN);
    }

    @Test
    void detectExtremeReturns_singleExtreme_isFlagged() {
        // 11 constant prices followed by a 400% spike (z ≈ 3.16, well above 3σ threshold).
        // The spike must be large enough that its z-score exceeds 3.0 even when the
        // outlier itself is included in the μ/σ estimation (conservative by design).
        List<CleanedRecord> records = buildRecords(
                new double[]{100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 500}
        );

        detector.detectExtremeReturns(records);

        CleanedRecord last = records.get(records.size() - 1);
        assertThat(last.getDataQuality()).isEqualTo(DataQuality.ANOMALY_FLAGGED);
        assertThat(last.getAnomalyType()).isEqualTo(AnomalyDetector.EXTREME_RETURN);
    }

    @Test
    void detectExtremeReturns_fewerThanTwoRecords_noChange() {
        CleanedRecord single = cleanRecord(LocalDate.of(2024, 1, 2), "100");
        List<CleanedRecord> records = new ArrayList<>(List.of(single));

        detector.detectExtremeReturns(records);

        assertThat(records.get(0).getDataQuality()).isEqualTo(DataQuality.CLEAN);
    }

    @Test
    void detectExtremeReturns_forwardFilledRecords_notFlagged() {
        // forward-filled record should not itself be flagged for EXTREME_RETURN
        CleanedRecord real1 = cleanRecord(LocalDate.of(2024, 1, 2), "100");
        CleanedRecord filled = filledRecord(LocalDate.of(2024, 1, 3), "100");
        CleanedRecord real2 = cleanRecord(LocalDate.of(2024, 1, 4), "200"); // big jump

        List<CleanedRecord> records = new ArrayList<>(List.of(real1, filled, real2));
        detector.detectExtremeReturns(records);

        // filled record must stay FORWARD_FILLED
        assertThat(records.get(1).getDataQuality()).isEqualTo(DataQuality.FORWARD_FILLED);
    }

    @Test
    void detectExtremeReturns_alreadyFlaggedRecord_anomalyTypeNotOverwritten() {
        // A record already ANOMALY_FLAGGED for ZERO_PRICE should not be changed to EXTREME_RETURN
        CleanedRecord base1 = cleanRecord(LocalDate.of(2024, 1, 2), "100");
        CleanedRecord base2 = cleanRecord(LocalDate.of(2024, 1, 3), "101");
        CleanedRecord already = cleanRecord(LocalDate.of(2024, 1, 4), "0.001");
        already.setDataQuality(DataQuality.ANOMALY_FLAGGED);
        already.setAnomalyType("ZERO_PRICE");

        // Add enough normal records to establish variance
        List<CleanedRecord> records = new ArrayList<>(List.of(base1, base2, already));
        detector.detectExtremeReturns(records);

        assertThat(records.get(2).getAnomalyType()).isEqualTo("ZERO_PRICE");
    }

    @Test
    void detectExtremeReturns_constantPrices_noFlagDueToZeroStdDev() {
        // If all prices equal → σ=0 → no anomaly flagged
        List<CleanedRecord> records = buildRecords(new double[]{100, 100, 100, 100, 100});
        detector.detectExtremeReturns(records);

        assertThat(records).allMatch(r -> r.getDataQuality() == DataQuality.CLEAN);
    }

    // Helpers
    private List<CleanedRecord> buildRecords(double[] prices) {
        List<CleanedRecord> list = new ArrayList<>();
        LocalDate base = LocalDate.of(2024, 1, 2);
        for (int i = 0; i < prices.length; i++) {
            list.add(cleanRecord(base.plusDays(i), String.valueOf(prices[i])));
        }
        return list;
    }

    private CleanedRecord cleanRecord(LocalDate date, String close) {
        CleanedRecord cr = new CleanedRecord();
        cr.setDate(date);
        cr.setClose(new BigDecimal(close));
        cr.setDataQuality(DataQuality.CLEAN);
        return cr;
    }

    private CleanedRecord filledRecord(LocalDate date, String close) {
        CleanedRecord cr = new CleanedRecord();
        cr.setDate(date);
        cr.setClose(new BigDecimal(close));
        cr.setDataQuality(DataQuality.FORWARD_FILLED);
        return cr;
    }
}
