package com.financial.etl.transform.cleaner;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeMap;

/**
 * Rellena fechas faltantes en la serie de un activo usando el calendario bursátil unificado.
 *
 * Estrategia:
 * - Para cada fecha del calendario unificado:
 *   · Si el activo tiene registro → conservarlo (preservando la calidad asignada por OhlcConsistencyChecker)
 *   · Si falta → forward-fill desde el último registro conocido → FORWARD_FILLED
 * - Si no existe registro previo (hueco al inicio) → backward-fill desde el primer registro futuro → FORWARD_FILLED
 *
 * Justificación del forward-fill:
 * Las series financieras son caminatas no aleatorias donde el último precio conocido es la mejor
 * estimación disponible para un día bursátil faltante (ej. festivo local no observado por otros mercados).
 * La eliminación rompería el alineamiento temporal requerido por los algoritmos posteriores.
 * La interpolación fue descartada porque los precios futuros no están disponibles en el momento de la transformación.
 *
 * Impacto algorítmico en análisis posteriores:
 * - VOLATILIDAD: Valores idénticos consecutivos producen retornos logarítmicos de cero, subestimando
 *   la volatilidad realizada. Las métricas de volatilidad deben excluir registros FORWARD_FILLED.
 * - CORRELACIÓN (Pearson): Rachas de valores rellenos inflan artificialmente la correlación entre pares
 *   de activos que comparten las mismas fechas faltantes, pudiendo agrupar activos no relacionados
 *   en la matriz de similitud.
 * - DETECCIÓN DE PATRONES: Los detectores de reversión a la media y momentum pueden generar señales
 *   falsas en rachas largas de valores rellenos. Los consumidores de patrones deben filtrar por
 *   DataQuality == CLEAN cuando se requiere alta precisión.
 * - ORDENAMIENTO: Los registros rellenos tienen fechas y precios válidos, por lo que participan
 *   normalmente en el ordenamiento por fecha y precio de cierre sin introducir anomalías.
 *
 * Complejidad: O(D) por activo.
 */
@Service
public class MissingValueCleaner {

    /**
     * Alinea los registros validados de un activo al calendario unificado.
     *
     * @param symbol           símbolo del activo
     * @param calendarDates    conjunto ordenado de fechas bursátiles unificadas
     * @param validatedRecords salida del OhlcConsistencyChecker (puede tener huecos)
     * @return lista alineada (un registro por fecha del calendario)
     */
    public List<CleanedRecord> fill(String symbol,
                                    NavigableSet<LocalDate> calendarDates,
                                    List<CleanedRecord> validatedRecords) {

        // Indexa los registros existentes por fecha para búsqueda O(1)
        TreeMap<LocalDate, CleanedRecord> byDate = new TreeMap<>();
        for (CleanedRecord cr : validatedRecords) {
            byDate.put(cr.getDate(), cr);
        }

        List<CleanedRecord> aligned = new ArrayList<>(calendarDates.size());
        CleanedRecord lastKnown = null;

        for (LocalDate date : calendarDates) {
            CleanedRecord existing = byDate.get(date);

            if (existing != null) {
                lastKnown = existing;
                aligned.add(existing);
            } else {
                // Es necesario rellenar esta fecha
                CleanedRecord fill;
                if (lastKnown != null) {
                    // Forward-fill desde el último registro conocido
                    fill = forwardFill(symbol, date, lastKnown);
                } else {
                    // Sin registro previo; se busca el primer registro futuro para backward-fill
                    CleanedRecord firstFuture = findFirstFuture(byDate, date);
                    if (firstFuture != null) {
                        fill = backwardFill(symbol, date, firstFuture);
                    } else {
                        // Sin datos para este activo — omitir
                        continue;
                    }
                }
                aligned.add(fill);
            }
        }

        return aligned;
    }

    private CleanedRecord forwardFill(String symbol, LocalDate date, CleanedRecord source) {
        CleanedRecord cr = new CleanedRecord();
        cr.setSymbol(symbol);
        cr.setDate(date);
        cr.setOpen(source.getOpen());
        cr.setHigh(source.getHigh());
        cr.setLow(source.getLow());
        cr.setClose(source.getClose());
        cr.setVolume(source.getVolume());
        cr.setDataQuality(DataQuality.FORWARD_FILLED);
        cr.setTransformedAt(LocalDateTime.now());
        return cr;
    }

    private CleanedRecord backwardFill(String symbol, LocalDate date, CleanedRecord source) {
        return forwardFill(symbol, date, source);
    }

    private CleanedRecord findFirstFuture(TreeMap<LocalDate, CleanedRecord> byDate, LocalDate date) {
        var entry = byDate.higherEntry(date);
        return entry != null ? entry.getValue() : null;
    }
}
