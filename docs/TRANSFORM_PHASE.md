# Fase de Transformación — ETL Service

## Índice

1. [Resumen](#resumen)
2. [Contexto del problema](#contexto-del-problema)
3. [Arquitectura del pipeline](#arquitectura-del-pipeline)
4. [Algoritmo 1 — Observable-Calendar (TradingCalendarService)](#algoritmo-1--observable-calendar-tradingcalendarservice)
5. [Algoritmo 2 — OHLC Consistency Check (OhlcConsistencyChecker)](#algoritmo-2--ohlc-consistency-check-ohlcconsistencychecker)
6. [Algoritmo 3 — Forward-Fill / Backward-Fill (MissingValueCleaner)](#algoritmo-3--forward-fill--backward-fill-missingvaluecleaner)
7. [Algoritmo 4 — Detección de anomalías por Z-score (AnomalyDetector)](#algoritmo-4--detección-de-anomalías-por-z-score-anomalydetector)
8. [Impacto algorítmico en análisis posteriores](#impacto-algorítmico-en-análisis-posteriores)
9. [Esquema de la tabla `cleaned_records`](#esquema-de-la-tabla-cleaned_records)
10. [Nuevos archivos](#nuevos-archivos)
11. [API REST](#api-rest)
12. [Cómo probar](#cómo-probar)
13. [Cobertura de tests](#cobertura-de-tests)

---

## Resumen

La fase de transformación toma los datos crudos almacenados en `financial_series` y `price_records` (descargados desde TwelveData en la fase de extracción) y los convierte en un dataset **limpio, alineado temporalmente y con metadatos de calidad**, persistido en la tabla `cleaned_records`.

El pipeline resuelve tres problemas fundamentales de los datos financieros multi-mercado:

| Problema | Causa | Técnica aplicada |
|----------|-------|-----------------|
| Calendarios de trading distintos por mercado | NYSE, LSE, TSE tienen distintos festivos | Observable-Calendar (unión de fechas observadas) |
| Fechas faltantes en series individuales | El mercado de un activo estuvo cerrado ese día | Forward-fill (LOCF) |
| Datos corruptos del proveedor | Errores de feed, splits no ajustados, glitches | OHLC check + Z-score sobre retornos logarítmicos |

---

## Contexto del problema

El dataset contiene activos de **múltiples mercados globales** (NYSE, NASDAQ, LSE, TSE, etc.). Cada mercado tiene su propio calendario de festivos locales. Esto genera dos problemas estructurales:

**Problema 1 — Desalineación temporal**

```
Fecha       AAPL (NYSE)   VOD.L (LSE)
2024-05-27  cerrado       147.20        ← Memorial Day USA, LSE abierto
2024-05-28  191.50        148.30
```

Si se construye la matriz de retornos directamente sobre los datos crudos, el día 2024-05-27 existe para VOD.L pero no para AAPL, lo que produce vectores de diferente longitud. Cualquier análisis que requiera matrices de covarianza, correlaciones o backtesting necesita que **todos los activos tengan exactamente las mismas fechas**.

**Problema 2 — Calidad de datos del proveedor**

Los proveedores de datos financieros pueden entregar:
- Precios `low > high` (inversión de campos en el feed)
- Precios `open` fuera del rango `[low, high]`
- Precios iguales a cero (error de datos no disponibles)
- Retornos de +50% o -40% en un día (event-driven o error del feed)

Estos registros, si llegan sin tratar a los algoritmos de análisis, introducen sesgos estadísticos severos.

---

## Arquitectura del pipeline

```
FinancialSeriesRepository.findAllWithPriceRecords()
            │
            ▼
┌─────────────────────────────────┐
│   TradingCalendarService        │  Paso 1
│   buildUnifiedCalendar()        │  → TreeSet<LocalDate> (fechas únicas, sin fines de semana)
└─────────────────────────────────┘
            │
            ▼  (por cada símbolo)
┌─────────────────────────────────┐
│   OhlcConsistencyChecker        │  Paso 2a
│   validate()                    │  → List<CleanedRecord> con calidades iniciales
└─────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────┐
│   MissingValueCleaner           │  Paso 2b
│   fill()                        │  → Lista alineada al calendario (sin huecos)
└─────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────┐
│   AnomalyDetector               │  Paso 2c
│   detectExtremeReturns()        │  → Retornos extremos marcados como ANOMALY_FLAGGED
└─────────────────────────────────┘
            │
            ▼
CleanedRecordRepository.deleteAll() + saveAll()
            │
            ▼
     TransformSummary (estadísticas del proceso)
```

Todas las operaciones se ejecutan **dentro de una única transacción** (`@Transactional` en `DataTransformService.transformAll()`), garantizando que la tabla `cleaned_records` nunca quede en un estado parcial: o se reemplaza completamente o se revierte.

---

## Algoritmo 1 — Observable-Calendar (`TradingCalendarService`)

### El problema que resuelve

Para alinear N series de activos de distintos mercados, se necesita un conjunto de fechas de referencia. Las dos opciones obvias son:

- **Intersección**: solo incluir fechas en las que **todos** los activos tienen dato.
- **Unión**: incluir fechas en las que **al menos un** activo tiene dato.

La intersección descarta información válida: si NYSE cierra el día de Thanksgiving pero LSE abre, la intersección elimina ese día y se pierde el movimiento de VOD.L. La unión es la opción correcta, pero requiere saber qué días son "de trading" a nivel global.

### La solución: calendario observable

En lugar de hardcodear calendarios por exchange (lo que requeriría mantener una base de datos de festivos por país, actualizada año a año), se usa una estrategia **auto-derivada de los propios datos**:

> **Regla:** Una fecha pertenece al calendario unificado si y solo si aparece en los datos descargados de **al menos un símbolo**, y no es sábado ni domingo.

```java
public TreeSet<LocalDate> buildUnifiedCalendar(List<FinancialSeries> allSeries) {
    TreeSet<LocalDate> calendar = new TreeSet<>();
    for (FinancialSeries series : allSeries) {
        for (PriceRecord record : series.getPriceRecords()) {
            LocalDate date = record.getDatetime();
            if (date != null && isNotWeekend(date)) {
                calendar.add(date);   // TreeSet descarta duplicados automáticamente
            }
        }
    }
    return calendar;
}
```

### Por qué funciona para multi-mercado

| Caso | Comportamiento |
|------|---------------|
| NYSE cerrado (festivo USA), LSE abierto | El día entra al calendario porque LSE lo reportó. AAPL recibirá forward-fill → correcto, su precio no cambió. |
| Todos los mercados cerrados (fin de semana) | Ningún símbolo reporta ese día → **no entra al calendario** → no se genera ningún registro sintético. |
| Todos los mercados cerrados (festivo global raro) | Ningún símbolo reporta ese día → no entra → correcto. |

### Estructura de datos: `TreeSet<LocalDate>`

Se usa `TreeSet` (Red-Black Tree internamente) porque:
1. **Descarte automático de duplicados**: si 20 símbolos reportan el mismo día, solo aparece una vez.
2. **Orden ascendente garantizado**: la iteración posterior en `MissingValueCleaner` requiere recorrer las fechas en orden cronológico.
3. **`NavigableSet` API**: permite operaciones como `higher()`, `lower()`, `subSet()` que se usan en el cleaner.

### Complejidad

```
Sea N = número de símbolos
Sea D = días promedio por serie (~1258 días para 5 años)

Iteración sobre todos los registros:  O(N · D)
Inserción en TreeSet por registro:    O(log(N · D))  [pero D_unique << N · D, boundea en O(D)]

Total:  O(N · D · log D)
```

En la práctica con 20 símbolos y 5 años de datos diarios (~25,000 registros totales), el calendario resulta en ~1,258 fechas únicas (días hábiles en 5 años), y la construcción toma microsegundos.

---

## Algoritmo 2 — OHLC Consistency Check (`OhlcConsistencyChecker`)

### El invariante OHLC

En cualquier vela (candlestick) financiera, los cuatro precios deben satisfacer por definición:

```
low  ≤  min(open, close)
max(open, close)  ≤  high
low  ≤  high
```

El rango `[low, high]` representa el intervalo de precios **observados durante la sesión**. `open` y `close` son el precio al inicio y al final de la sesión, por lo que deben estar dentro de ese rango. Cualquier violación indica un error en el feed del proveedor.

### Árbol de decisión del checker

```
¿Algún precio ≤ 0?
    SÍ → ANOMALY_FLAGGED  (anomalyType = ZERO_PRICE)
         No se corrige: precio cero es físicamente imposible y puede indicar
         error sistémico del proveedor. Se preserva para auditoría.
    NO ↓

¿low > high? (inversión simple de campos)
    SÍ → swap(low, high) → ANOMALY_CORRECTED  (anomalyType = OHLC_INCONSISTENCY)
         Corrección de distorsión mínima: solo se invirtieron dos campos.
         Se guarda original_close para trazabilidad.
    NO ↓

¿open < low || open > high || close < low || close > high?
    SÍ → ANOMALY_FLAGGED  (anomalyType = OHLC_INCONSISTENCY)
         La inconsistencia es más compleja (no es simple inversión).
         Corrección automática podría enmascarar el error real.
    NO ↓

→ CLEAN
```

### Por qué no se corrigen todas las inconsistencias

La filosofía del checker es **corrección mínima con máxima trazabilidad**:

- **Solo se corrige `low > high`** porque es el único caso donde la corrección es inequívoca (los campos fueron invertidos) y reversible (se guarda `original_close`).
- **El resto se marca pero no se toca**: si `open > high`, podría deberse a un gap de apertura extremo, un ajuste por dividendo no aplicado correctamente, o un error del feed. Corregir automáticamente podría eliminar información real.

Los registros `ANOMALY_FLAGGED` permanecen en `cleaned_records` con sus valores originales, accesibles para auditoría humana.

### Complejidad

```
O(1) por registro → O(D) por símbolo → O(N · D) total
```

---

## Algoritmo 3 — Forward-Fill / Backward-Fill (`MissingValueCleaner`)

### El problema

Después del paso 2a, cada símbolo tiene como máximo `D_symbol` registros, donde `D_symbol ≤ D_calendar`. Los días que el exchange de ese símbolo estuvo cerrado (festivos locales) no tienen registro. Para producir una matriz alineada (N símbolos × D_calendar días), hay que **rellenar los huecos**.

### Forward-Fill (LOCF — Last Observation Carried Forward)

```
Calendario:  [Lun, Mar, Mié, Jue, Vie]
AAPL:        [100,  ---,  101,  ---,  102]
            ↓  forward-fill
AAPL:        [100,  100,  101,  101,  102]
```

**Semántica financiera correcta**: cuando el mercado está cerrado el martes (festivo en NYSE), el precio "vigente" o "implícito" de AAPL es el último precio de cierre conocido (el del lunes). El inversor que quisiera valorar su portfolio ese martes usaría exactamente ese precio. Por lo tanto, forward-fill no introduce información ficticia — reproduce el estado real del mercado.

**Por qué no interpolación lineal**: la interpolación lineal supondría que el precio se movió de forma continua entre lunes y miércoles, lo que introduce **look-ahead bias** (usa información futura para rellenar el presente) y distorsiona los retornos calculados posteriormente.

### Implementación con `TreeMap`

```java
// Indexar registros existentes por fecha: O(D_symbol · log D_symbol)
TreeMap<LocalDate, CleanedRecord> byDate = new TreeMap<>();
for (CleanedRecord cr : validatedRecords) {
    byDate.put(cr.getDate(), cr);
}

// Recorrer el calendario en orden: O(D_calendar)
CleanedRecord lastKnown = null;
for (LocalDate date : calendarDates) {            // calendarDates es ya un TreeSet ordenado
    CleanedRecord existing = byDate.get(date);    // O(log D_symbol) lookup
    if (existing != null) {
        lastKnown = existing;
        aligned.add(existing);
    } else {
        if (lastKnown != null) {
            aligned.add(forwardFill(symbol, date, lastKnown));
        } else {
            // Hueco al inicio: backward-fill con el primer dato disponible
            CleanedRecord firstFuture = byDate.higherEntry(date).getValue(); // O(log D_symbol)
            aligned.add(backwardFill(symbol, date, firstFuture));
        }
    }
}
```

La elección de `TreeMap` sobre `HashMap` permite usar `higherEntry(date)` para encontrar el primer registro futuro en O(log D) sin iterar linealmente, lo que es necesario para el backward-fill al inicio de la serie.

### Backward-Fill (solo al inicio)

Si una serie tiene datos desde 2021-03-15 pero el calendario unificado comienza en 2021-01-04 (porque otro símbolo sí tiene datos desde entonces), los días 2021-01-04 a 2021-03-14 para ese símbolo se rellenan con el **primer dato disponible** (2021-03-15). Esto es menos preciso que el forward-fill, pero es preferible a dejar huecos que romperían la dimensionalidad de la matriz.

Ambos casos (forward y backward fill) se marcan como `FORWARD_FILLED` para que los algoritmos posteriores puedan excluirlos si la situación lo requiere.

### Complejidad

```
Construcción del TreeMap:  O(D_symbol · log D_symbol)
Recorrido del calendario:  O(D_calendar · log D_symbol)

Total por símbolo:  O(D · log D)
Total global:       O(N · D · log D)
```

---

## Algoritmo 4 — Detección de anomalías por Z-score (`AnomalyDetector`)

### Por qué retornos logarítmicos y no precios absolutos

Para detectar movimientos de precios anómalos, no se analiza el precio en sí sino el **retorno logarítmico diario**:

```
r_t = ln(close_t / close_{t-1})
```

**Razones matemáticas y financieras:**

1. **Normalización del nivel de precio**: un movimiento de $1 en AAPL (precio ~$190) y en AMZN (precio ~$180) tiene distinto significado proporcional. Los retornos logarítmicos son independientes del nivel de precio.

2. **Aditividad temporal**: `r_lunes + r_martes + r_miercoles = ln(close_mié / close_vie_pasado)`. Los retornos logarítmicos se pueden sumar directamente para obtener el retorno acumulado.

3. **Aproximación a normalidad**: bajo el modelo Black-Scholes estándar, los retornos logarítmicos se asumen normalmente distribuidos, lo que hace que el Z-score sea estadísticamente apropiado.

4. **Simetría**: una caída del 50% (`r = ln(0.5) ≈ -0.693`) y una subida del 100% (`r = ln(2) ≈ 0.693`) tienen el mismo valor absoluto, reflejando equivalencia de magnitud.

### El algoritmo en dos pasadas

**Pasada 1 — Estimación de μ y σ**

Se calculan la media y desviación estándar de los retornos usando **solo registros NO forward-filled** y de pares consecutivos reales:

```java
for (CleanedRecord cr : records) {
    boolean isReal = cr.getDataQuality() != DataQuality.FORWARD_FILLED;

    if (prevClose != null && prevWasReal && isReal && cr.getClose() != null) {
        double logReturn = Math.log(cr.getClose() / prevClose);
        sumR  += logReturn;
        sumR2 += logReturn * logReturn;
        count++;
    }

    if (isReal) {
        prevClose = cr.getClose();
        prevWasReal = true;
    } else {
        prevWasReal = false;  // resetear: no computar retorno sobre filled
    }
}

double mean = sumR / count;
double variance = (sumR2 / count) - (mean * mean);   // E[X²] - E[X]²
double stdDev = Math.sqrt(variance);
```

**¿Por qué excluir los forward-filled del cálculo de μ y σ?**

Un día forward-filled tiene retorno 0 (precio = precio anterior). Incluir muchos ceros sesgaría la media hacia abajo y reduciría artificialmente la desviación estándar, haciendo que el Z-score posterior sea menos sensible.

**Pasada 2 — Scoring y flagging**

```java
for (CleanedRecord cr : records) {
    if (lastRealClose != null && cr.getClose() != null) {
        double logReturn = Math.log(cr.getClose() / lastRealClose);
        double zScore = (logReturn - mean) / stdDev;

        if (Math.abs(zScore) > 3.0) {
            if (cr.getDataQuality() == CLEAN || cr.getDataQuality() == ANOMALY_CORRECTED) {
                cr.setDataQuality(ANOMALY_FLAGGED);
                cr.setAnomalyType("EXTREME_RETURN");
            }
        }
    }
    if (cr.getDataQuality() != FORWARD_FILLED) {
        lastRealClose = cr.getClose();
    }
}
```

### Por qué umbral de 3.0σ

Bajo distribución normal, la probabilidad de que un retorno supere 3σ es **~0.27%** (≈ 1 día cada 390 días hábiles, es decir, aproximadamente una vez cada 1.5 años). En datos financieros reales (distribuciones de cola gruesa, *fat tails*), este umbral es **conservador**: captura eventos genuinamente extremos (flash crashes, anuncios de earnings, splits no ajustados) sin generar falsos positivos en volatilidad normal.

Umbrales más bajos (2σ) generarían demasiados falsos positivos en activos volátiles como TSLA o NVDA. Umbrales más altos (4σ) perderían errores reales del feed.

### Por qué se marca pero NO se corrige

A diferencia del OHLC checker, los registros con retornos extremos **no son modificados**. Las razones:

1. **Puede ser un evento corporativo real**: anuncio de earnings, merger, reverse split. Eliminar o suavizar el dato sería incorrecto.
2. **Puede ser un split no ajustado**: el `algorithm-service` debería manejar este caso explícitamente, no el ETL.
3. **Preservación para auditoría**: el usuario puede consultarlo, investigar y decidir si excluirlo del análisis.

Los registros `ANOMALY_FLAGGED` permanecen en `cleaned_records` y los análisis posteriores pueden filtrarlos con `WHERE data_quality != 'ANOMALY_FLAGGED'` si así lo requieren.

### Complejidad

```
2 pasadas lineales sobre D_calendar registros: O(D) por símbolo
Total global: O(N · D)
```

---

## Impacto algorítmico en análisis posteriores

Esta sección es la más importante: las decisiones de transformación tienen consecuencias directas en la corrección de cualquier análisis que el `algorithm-service` ejecute.

### 1. Matrices de covarianza y correlación

Para calcular la correlación entre AAPL y VOD.L, ambos vectores de retornos deben tener la **misma longitud y las mismas fechas**. El Observable-Calendar + Forward-Fill garantiza que `cleaned_records` tiene exactamente `D_calendar` registros por cada símbolo, haciendo posible:

```sql
-- Todos los símbolos tienen el mismo número de filas para el mismo rango de fechas
SELECT symbol, COUNT(*) FROM cleaned_records GROUP BY symbol;
-- AAPL  1258
-- VOD.L 1258
-- TSLA  1258
```

Sin esta alineación, el cálculo de la matriz de covarianza N×N requeriría manejo especial de valores faltantes con imputación sofisticada, o simplemente produciría resultados incorrectos.

### 2. Backtesting de estrategias

Un backtest que simula comprar/vender activos en fechas específicas necesita que todos los activos tengan precio en todas las fechas. Sin forward-fill, una estrategia que intente comprar HSBC.L el Día de Año Nuevo encontraría un `NULL`, lo que requeriría lógica de manejo de errores en cada estrategia.

Con forward-fill, el backtest recibe automáticamente el precio correcto ("no hubo trading, el precio es el del día anterior") sin código adicional.

### 3. Algoritmos de series temporales (ARIMA, GARCH)

Estos modelos asumen que la serie temporal no tiene huecos. Un gap de 3 días en una serie haría que el modelo calculara un retorno de 3 días como si fuera un retorno de 1 día, distorsionando los coeficientes estimados. El forward-fill elimina este problema.

### 4. Detección de anomalías y gestión de riesgo

Los registros `ANOMALY_FLAGGED` (tanto OHLC como EXTREME_RETURN) permiten al `algorithm-service` implementar **exclusión selectiva**:

```sql
-- Calcular VaR usando solo datos limpios
SELECT symbol, date, close FROM cleaned_records
WHERE data_quality IN ('CLEAN', 'FORWARD_FILLED', 'ANOMALY_CORRECTED')
ORDER BY symbol, date;

-- Auditar qué días tuvieron movimientos extremos
SELECT symbol, date, close, anomaly_type FROM cleaned_records
WHERE data_quality = 'ANOMALY_FLAGGED'
ORDER BY date;
```

Si los datos crudos se usaran directamente, un día con un precio erróneo de $0 introduciría un retorno de -100% en los cálculos de VaR o Expected Shortfall, produciendo métricas de riesgo completamente distorsionadas.

### 5. Optimización de portafolios (Markowitz)

La frontera eficiente de Markowitz requiere la matriz de covarianza `Σ` de los retornos. Esta matriz debe ser **positiva semidefinida** para que el problema de optimización cuadrática tenga solución. Una serie con huecos o con outliers extremos puede hacer que `Σ` sea indefinida numéricamente. La transformación garantiza series completas y estadísticamente consistentes.

---

## Esquema de la tabla `cleaned_records`

```sql
CREATE TABLE cleaned_records (
    id             BIGSERIAL PRIMARY KEY,
    symbol         VARCHAR(20)    NOT NULL,
    date           DATE           NOT NULL,
    open           NUMERIC(18,6),
    high           NUMERIC(18,6),
    low            NUMERIC(18,6),
    close          NUMERIC(18,6),
    volume         BIGINT,
    data_quality   VARCHAR(30)    NOT NULL,   -- CLEAN | FORWARD_FILLED | ANOMALY_CORRECTED | ANOMALY_FLAGGED
    anomaly_type   VARCHAR(100),              -- OHLC_INCONSISTENCY | EXTREME_RETURN | ZERO_PRICE | NULL
    original_close NUMERIC(18,6),             -- Valor antes de corrección (solo ANOMALY_CORRECTED)
    transformed_at TIMESTAMP      NOT NULL,
    UNIQUE (symbol, date)
);
```

La tabla se crea automáticamente al iniciar el servicio (`spring.jpa.hibernate.ddl-auto=update`).

El campo `data_quality` actúa como **metadato de linaje de datos**: cualquier análisis posterior puede saber exactamente de dónde vino cada valor y con qué confianza.

---

## Nuevos archivos

### Código de producción

```
etl-service/src/main/java/com/financial/etl/
├── entity/
│   ├── DataQuality.java                        Enum con los 4 niveles de calidad
│   └── CleanedRecord.java                      Entidad JPA → tabla cleaned_records
├── repository/
│   └── CleanedRecordRepository.java            JPA repo + proyección countByDataQuality()
├── dto/
│   └── TransformSummary.java                   DTO de respuesta con estadísticas
├── transform/
│   ├── DataTransformService.java               Orquestador del pipeline (@Transactional)
│   ├── calendar/
│   │   └── TradingCalendarService.java         Observable-Calendar Approach
│   └── cleaner/
│       ├── OhlcConsistencyChecker.java         Validación y corrección OHLC
│       ├── MissingValueCleaner.java            Forward-fill / Backward-fill
│       └── AnomalyDetector.java               Z-score sobre retornos logarítmicos
└── controller/
    └── TransformController.java               POST /transform · GET /transform/status
```

### Modificaciones a archivos existentes

- `FinancialSeriesRepository.java`: añadido `findAllWithPriceRecords()` con `LEFT JOIN FETCH` para evitar N+1 queries al cargar las series con sus price records.

### Tests unitarios

```
etl-service/src/test/java/com/financial/etl/
├── transform/
│   ├── calendar/
│   │   └── TradingCalendarServiceTest.java     6 tests
│   └── cleaner/
│       ├── OhlcConsistencyCheckerTest.java     8 tests
│       ├── MissingValueCleanerTest.java        6 tests
│       └── AnomalyDetectorTest.java            6 tests
├── service/
│   └── DataTransformServiceTest.java           3 tests
└── controller/
    └── TransformControllerTest.java            3 tests
```

---

## API REST

### `POST /api/etl/transform`

Ejecuta el pipeline completo sobre todos los datos disponibles en `financial_series`. La operación es idempotente: cada ejecución reemplaza completamente la tabla `cleaned_records`.

**Request:** ningún body requerido.

**Response `202 Accepted`:**
```json
{
  "symbolsProcessed": 20,
  "calendarDays": 1258,
  "totalRecords": 25160,
  "cleanRecords": 24850,
  "forwardFilledRecords": 247,
  "anomalyCorrectedRecords": 8,
  "anomalyFlaggedRecords": 55,
  "transformedAt": "2026-02-21T18:45:00.123"
}
```

### `GET /api/etl/transform/status`

Retorna los conteos actuales de `cleaned_records` agrupados por nivel de calidad. Útil para monitoreo sin re-ejecutar la transformación.

**Response `200 OK`:**
```json
{
  "CLEAN": 24850,
  "FORWARD_FILLED": 247,
  "ANOMALY_CORRECTED": 8,
  "ANOMALY_FLAGGED": 55
}
```

---

## Cómo probar

### Prerrequisitos

1. PostgreSQL corriendo (`docker-compose up -d`)
2. El batch download previo ejecutado (tabla `price_records` con datos)
3. ETL Service levantado:
   ```bash
   ./mvnw.cmd spring-boot:run -pl etl-service
   ```

### Con Postman

**1. Ejecutar transformación:**
```
POST http://localhost:8082/api/etl/transform
```
No requiere body. Esperar `202 Accepted`.

**2. Consultar estado:**
```
GET http://localhost:8082/api/etl/transform/status
```

### Con curl

```bash
# Ejecutar pipeline
curl -X POST http://localhost:8082/api/etl/transform

# Consultar resultado
curl http://localhost:8082/api/etl/transform/status
```

### Verificación directa en PostgreSQL

```sql
-- Distribución de calidad
SELECT data_quality, COUNT(*) AS total
FROM cleaned_records
GROUP BY data_quality
ORDER BY total DESC;

-- Ver registros con anomalías
SELECT symbol, date, open, high, low, close, anomaly_type, original_close
FROM cleaned_records
WHERE data_quality IN ('ANOMALY_CORRECTED', 'ANOMALY_FLAGGED')
ORDER BY date;

-- Verificar alineación: todos los símbolos deben tener el mismo número de registros
SELECT symbol, COUNT(*) AS days
FROM cleaned_records
GROUP BY symbol
ORDER BY days;

-- Ver retornos extremos detectados
SELECT symbol, date, close, anomaly_type
FROM cleaned_records
WHERE anomaly_type = 'EXTREME_RETURN'
ORDER BY symbol, date;
```

### Tests unitarios

```bash
# Solo el módulo ETL
./mvnw.cmd test -pl etl-service

# Solo las nuevas clases de transformación
./mvnw.cmd test -pl etl-service -Dtest="TradingCalendarServiceTest,OhlcConsistencyCheckerTest,MissingValueCleanerTest,AnomalyDetectorTest,DataTransformServiceTest,TransformControllerTest"
```

---

## Cobertura de tests

| Clase | Tests | Casos cubiertos |
|-------|-------|----------------|
| `TradingCalendarService` | 6 | Exclusión de fines de semana, merge multi-series, deduplicación, lista vacía, ordenamiento ascendente |
| `OhlcConsistencyChecker` | 8 | Registro limpio, inversión low/high, precio cero, precio negativo, open>high, close<low, múltiples registros, lista vacía |
| `MissingValueCleaner` | 6 | Sin huecos, hueco en medio (forward-fill), hueco al inicio (backward-fill), símbolo sin datos, calendario vacío, preservación de calidad previa |
| `AnomalyDetector` | 6 | Retornos normales, spike extremo (z>3σ), menos de 2 registros, forward-filled no flaggeable, calidad existente no sobreescrita, stdDev=0 |
| `DataTransformService` | 3 | Pipeline completo, serie vacía con ceros, conteo correcto por calidad |
| `TransformController` | 3 | POST 202 con summary, GET status con datos, GET status vacío |
| **Total nuevos** | **29** | |
| **Total módulo** | **78** | |
