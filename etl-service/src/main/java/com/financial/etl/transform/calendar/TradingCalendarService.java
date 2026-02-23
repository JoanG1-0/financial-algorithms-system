package com.financial.etl.transform.calendar;

import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.entity.PriceRecord;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;

/**
 * Builds a unified trading calendar from observed data across all symbols.
 *
 * Strategy: Observable-Calendar Approach.
 * A date is included in the unified calendar if and only if it appears in the
 * downloaded data of at least one symbol, and it is not a weekend.
 *
 * Complexity: O(N·D) where N = number of symbols, D = average days per series.
 */
@Service
public class TradingCalendarService {

    /**
     * Builds a unified, sorted set of trading dates from all series.
     *
     * @param allSeries all financial series with their price records loaded
     * @return NavigableSet of dates (ascending), excluding weekends
     */
    public TreeSet<LocalDate> buildUnifiedCalendar(List<FinancialSeries> allSeries) {
        TreeSet<LocalDate> calendar = new TreeSet<>();

        for (FinancialSeries series : allSeries) {
            for (PriceRecord record : series.getPriceRecords()) {
                LocalDate date = record.getDatetime();
                if (date != null && isNotWeekend(date)) {
                    calendar.add(date);
                }
            }
        }

        return calendar;
    }

    private boolean isNotWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }
}
