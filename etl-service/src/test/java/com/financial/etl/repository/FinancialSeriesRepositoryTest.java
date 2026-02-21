package com.financial.etl.repository;

import com.financial.etl.entity.FinancialSeries;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FinancialSeriesRepositoryTest {

    @Autowired
    private FinancialSeriesRepository repository;

    @Test
    void findBySymbol_returnsMatchingRecords() {
        FinancialSeries aapl = new FinancialSeries();
        aapl.setSymbol("AAPL");
        aapl.setInterval("1day");
        repository.save(aapl);

        FinancialSeries msft = new FinancialSeries();
        msft.setSymbol("MSFT");
        msft.setInterval("1day");
        repository.save(msft);

        List<FinancialSeries> result = repository.findBySymbol("AAPL");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void findBySymbol_returnsEmptyWhenNotFound() {
        List<FinancialSeries> result = repository.findBySymbol("UNKNOWN");
        assertThat(result).isEmpty();
    }

    @Test
    void findByBatchId_returnsMatchingRecords() {
        FinancialSeries fs = new FinancialSeries();
        fs.setSymbol("AAPL");
        fs.setBatchId(42L);
        repository.save(fs);

        FinancialSeries other = new FinancialSeries();
        other.setSymbol("MSFT");
        other.setBatchId(99L);
        repository.save(other);

        List<FinancialSeries> result = repository.findByBatchId(42L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void findByBatchId_returnsEmpty_whenNoBatchIdMatches() {
        List<FinancialSeries> result = repository.findByBatchId(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void findFirstBySymbol_returnsExisting_whenSymbolExists() {
        FinancialSeries aapl = new FinancialSeries();
        aapl.setSymbol("AAPL");
        repository.save(aapl);

        Optional<FinancialSeries> result = repository.findFirstBySymbol("AAPL");

        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void findFirstBySymbol_returnsEmpty_whenSymbolDoesNotExist() {
        Optional<FinancialSeries> result = repository.findFirstBySymbol("UNKNOWN");
        assertThat(result).isEmpty();
    }
}
