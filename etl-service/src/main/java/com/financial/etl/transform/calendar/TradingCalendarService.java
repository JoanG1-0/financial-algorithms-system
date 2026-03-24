package com.financial.etl.transform.calendar;

import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.entity.PriceRecord;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Construye un calendario bursátil unificado a partir de los datos observados de todos los activos.
 *
 * Estrategia: Enfoque de Calendario Observable (Observable-Calendar Approach).
 * Una fecha se incluye en el calendario unificado si y solo si aparece en los datos
 * descargados de al menos un activo y no es fin de semana.
 *
 * Complejidad: O(N·D) donde N = número de activos, D = días promedio por serie.
 */
@Service
public class TradingCalendarService {

    /**
     * Construye un conjunto ordenado y unificado de fechas bursátiles a partir de todas las series.
     *
     * @param allSeries todas las series financieras con sus registros de precio cargados
     * @return NavigableSet de fechas (ascendente), excluyendo fines de semana
     */
    public NavigableSet<LocalDate> buildUnifiedCalendar(List<FinancialSeries> allSeries) {
        TreeSet<LocalDate> calendar = new TreeSet<>();

        for (FinancialSeries series : allSeries) {
            for (PriceRecord price : series.getPriceRecords()) {
                LocalDate date = price.getDatetime();
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
