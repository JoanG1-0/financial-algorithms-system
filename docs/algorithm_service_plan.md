# README: Algorithm Service — Plan de Implementación

> Archivo de referencia para seguir la implementación módulo a módulo en sesiones futuras.
> Ubicación destino: `docs/algorithm_service_plan.md`

---

## Contexto

El `algorithm-service` (puerto 8083) es un microservicio del sistema financiero que consume los datos limpios producidos por el `etl-service` y ejecuta análisis algorítmicos sobre ellos. Actualmente solo tiene `AlgorithmServiceApplication.java` — está vacío.

**Restricción crítica:** todos los algoritmos deben implementarse manualmente desde sus fundamentos matemáticos en Java. Está prohibido usar librerías externas de cálculo (Apache Commons Math, Weka, etc.).

**Persistencia:** los resultados se guardan en `algorithm_db` (ya existe en docker-compose). El `pom.xml` del servicio ya tiene `spring-boot-starter-data-jpa` + `postgresql`.

**Comunicación inter-servicios:** el algorithm-service llama al etl-service via HTTP REST usando un `EtlServiceClient` (RestTemplate), igual que `TwelveDataHttpClient` en ETL.

---

## Estructura de paquetes destino

```
algorithm-service/src/main/java/com/financial/algorithm/
├── similarity/
│   ├── EuclideanDistance.java
│   ├── PearsonCorrelation.java
│   ├── DynamicTimeWarping.java
│   └── CosineSimilarity.java
├── patterns/
│   ├── SlidingWindow.java
│   ├── PatternConsecutiveUp.java
│   └── PatternMeanReversion.java        ← depende de SimpleMovingAverage
├── risk/
│   ├── VolatilityCalculator.java
│   └── RiskClassifier.java              ← depende de VolatilityCalculator
├── indicators/
│   └── SimpleMovingAverage.java
├── dto/
│   ├── SimilarityResult.java
│   ├── PatternOccurrence.java
│   ├── PatternResult.java
│   ├── RiskProfile.java
│   └── VolatilityResult.java
├── entity/
│   ├── SimilarityRecord.java
│   ├── RiskRecord.java
│   ├── PatternRecord.java
│   └── SmaRecord.java
├── repository/
│   ├── SimilarityRepository.java
│   ├── RiskRepository.java
│   ├── PatternRepository.java
│   └── SmaRepository.java
├── service/
│   └── AlgorithmService.java
├── client/
│   └── EtlServiceClient.java
└── controller/
    └── AlgorithmController.java
```

---

## Árbol de dependencias entre fases

```
FASE 0 (Setup)
    ├── FASE 1 (SMA)  ←────────────────────────── base para Fase 3
    │       └── FASE 3 (Patrones — usa SMA)
    ├── FASE 2 (Similitud — independiente)
    └── FASE 4 (Riesgo — independiente)
              FASE 5 (Persistencia — requiere DTOs de Fases 1-4)
                      └── FASE 6 (Integración — requiere todo lo anterior)
```

---

## FASE 0 — Setup del módulo

**Estado:** pendiente

**Archivos a modificar:**

1. `algorithm-service/pom.xml`
   - Agregar Lombok `<optional>true</optional>`

2. `algorithm-service/src/main/resources/application.properties`
   - Agregar `spring.application.name=algorithm-service`
   - Agregar `spring.datasource.url=jdbc:postgresql://localhost:5432/algorithm_db`
   - Agregar `spring.datasource.username=financial_user`
   - Agregar `spring.datasource.password=financial_pass`
   - Agregar `spring.jpa.hibernate.ddl-auto=update`
   - Agregar `etl.service.url=http://localhost:8082`

3. `etl-service/.../controller/TransformController.java`
   - Agregar endpoint `GET /api/etl/transform/cleaned-prices` que retorna `Map<String, double[]>`
   - Este endpoint consulta `CleanedRecord` por symbol, extrae precios de cierre ordenados por fecha

**Checkpoint:**
```bash
./mvnw.cmd clean install -DskipTests -pl algorithm-service
```

---

## FASE 1 — Módulo: Indicadores Técnicos

**Estado:** pendiente
**Va primero** porque `PatternMeanReversion` lo necesita.

### `indicators/SimpleMovingAverage.java`

```
Método: double[] compute(double[] prices, int w)
Fórmula: SMA_t = (1/w) · Σ P_i  para i = t-w+1 ... t
Implementación: O(n) con suma deslizante
  - sum += prices[i] - prices[i-w] para cada i >= w
Primeros w-1 valores = Double.NaN
```

**Tests (`SimpleMovingAverageTest`):**
- `compute_returnsNaN_forFirstWMinusOnePositions()`
- `compute_correctValues_forWindowOf3()`
- `compute_entireArrayNaN_whenWindowLargerThanSeries()`
- `compute_singleElement_windowOne_returnsValue()`

**Checkpoint:**
```bash
./mvnw.cmd test -pl algorithm-service -Dtest=SimpleMovingAverageTest
```

---

## FASE 2 — Módulo: Similitud

**Estado:** pendiente

Todas las clases reciben retornos diarios (no precios crudos). Transformación:
```
r_i = (p_i - p_(i-1)) / p_(i-1)
```

### `similarity/EuclideanDistance.java`
```
Método: double compute(double[] a, double[] b)
Fórmula: sqrt( Σ (a_i - b_i)² )
Edge cases: longitudes distintas → IllegalArgumentException, arrays vacíos → 0.0
Complejidad: O(n) temporal, O(1) espacial
```

### `similarity/PearsonCorrelation.java`
```
Método: double compute(double[] x, double[] y)
Fórmula: Σ[(x_i-x̄)(y_i-ȳ)] / sqrt( Σ(x_i-x̄)² · Σ(y_i-ȳ)² )
Edge cases: σ=0 en alguna serie → retorna Double.NaN
Complejidad: O(n) temporal, O(1) espacial
```

### `similarity/DynamicTimeWarping.java`
```
Método: double compute(double[] a, double[] b)
Fórmula: M[i][j] = |a_i - b_j| + min(M[i-1][j], M[i][j-1], M[i-1][j-1])
Retorna: M[n][m] — costo acumulado de alineación óptima
Complejidad: O(n×m) temporal y espacial
NOTA: el más costoso — 190 pares para 20 activos
```

### `similarity/CosineSimilarity.java`
```
Método: double compute(double[] x, double[] y)
Fórmula: (x·y) / (|x| · |y|)
Edge cases: vector cero → retorna Double.NaN
Complejidad: O(n) temporal, O(1) espacial
```

**DTO a crear:** `dto/SimilarityResult.java`
```
campos: String tickerA, String tickerB,
        double euclidean, double pearson, double dtw, double cosine
patrón: builder manual (como TransformSummary en ETL)
```

**Tests:**
- `EuclideanDistanceTest` — series iguales→0, longitudes distintas→exception
- `PearsonCorrelationTest` — perfecta=1, inversa=-1, constante→NaN
- `DynamicTimeWarpingTest` — series iguales, series con desfase temporal
- `CosineSimilarityTest` — paralelos→1, opuestos→-1, ortogonales→0

**Checkpoint:**
```bash
./mvnw.cmd test -pl algorithm-service -Dtest=EuclideanDistanceTest,PearsonCorrelationTest,DynamicTimeWarpingTest,CosineSimilarityTest
```

---

## FASE 3 — Módulo: Detección de Patrones

**Estado:** pendiente
**Requiere:** FASE 1 (SimpleMovingAverage)

### `patterns/SlidingWindow.java`
```
Método: List<WindowSlice> extract(double[] series, int k)
Record interno: WindowSlice { int startIndex; double[] values; }
Produce (n-k+1) ventanas. Si k>n → lista vacía.
Complejidad: O(n) temporal, O(k) espacial por ventana
```

### `patterns/PatternConsecutiveUp.java`
```
Método: PatternResult detect(double[] closePrices, int k)
Lógica: calcula retornos → SlidingWindow(k) → cuenta ventanas con todos retornos > 0
Retorna: count, relativeFrequency (count / totalWindows), List<Integer> startIndices
```

### `patterns/PatternMeanReversion.java`
```
Método: PatternResult detect(double[] closePrices, int w, double threshold)
Lógica: en cada t calcula SMA_w(t) y σ_w(t) → detecta |close_t - SMA| > threshold·σ
Usa: SimpleMovingAverage internamente
Retorna: count, relativeFrequency, List<PatternOccurrence>
```

**DTOs a crear:**
```
PatternOccurrence: startIndex, price, sma, sigma, direction (ABOVE/BELOW)
PatternResult: symbol, patternType, totalWindows, occurrences, relativeFrequency, List<PatternOccurrence>
```

**Tests:**
- `SlidingWindowTest` — n=5 k=3→3 ventanas, k>n→vacío, k=n→1 ventana
- `PatternConsecutiveUpTest` — 5 días al alza, mix, todo a la baja
- `PatternMeanReversionTest` — precios estables→sin señales, spike→señal detectada

**Checkpoint:**
```bash
./mvnw.cmd test -pl algorithm-service -Dtest=SlidingWindowTest,PatternConsecutiveUpTest,PatternMeanReversionTest
```

---

## FASE 4 — Módulo: Volatilidad y Riesgo

**Estado:** pendiente

### `risk/VolatilityCalculator.java`
```
Método: VolatilityResult compute(double[] closePrices)
Fórmulas:
  r_i = ln(close_i / close_(i-1))       ← retorno logarítmico (Math.log)
  σ_diaria = sqrt( (1/n) · Σ(r_i - r̄)² )
  σ_anual  = σ_diaria · sqrt(252)        ← 252 días hábiles bursátiles
Edge case: menos de 2 precios → excepción o resultado vacío
```

### `risk/RiskClassifier.java`
```
Método: List<RiskProfile> classify(Map<String, double[]> portfolio)
Lógica: por cada ticker → llama VolatilityCalculator → asigna categoría → ordena ASC por σ_anual
Categorías:
  CONSERVATIVE : σ_anual < 0.15
  MODERATE     : 0.15 ≤ σ_anual ≤ 0.30
  AGGRESSIVE   : σ_anual > 0.30
```

**DTOs a crear:**
```
VolatilityResult: double dailySigma, double annualizedSigma, double[] logReturns
RiskProfile:      String ticker, double annualizedVolatility, RiskCategory category
RiskCategory:     enum { CONSERVATIVE, MODERATE, AGGRESSIVE }
```

**Tests:**
- `VolatilityCalculatorTest` — precios constantes→σ=0, precios con varianza conocida→valor exacto
- `RiskClassifierTest` — 3 activos en distintas categorías, orden ascendente correcto

**Checkpoint:**
```bash
./mvnw.cmd test -pl algorithm-service -Dtest=VolatilityCalculatorTest,RiskClassifierTest
```

---

## FASE 5 — Persistencia (Entidades + Repositorios)

**Estado:** pendiente
**Requiere:** Fases 1-4 completas y en verde

**Entidades JPA en `algorithm_db`:**

| Entidad | Tabla | Campos |
|---------|-------|--------|
| `SimilarityRecord` | `similarity_results` | id, tickerA, tickerB, euclidean, pearson, dtw, cosine, computedAt |
| `RiskRecord` | `risk_profiles` | id, ticker, annualizedVolatility, category (STRING), computedAt |
| `PatternRecord` | `pattern_results` | id, symbol, patternType, occurrences, relativeFrequency, indicesJson (TEXT), computedAt |
| `SmaRecord` | `sma_results` | id, symbol, window, valuesJson (TEXT), computedAt |

**Repositorios Spring Data JPA:**
- `SimilarityRepository extends JpaRepository<SimilarityRecord, Long>`
- `RiskRepository extends JpaRepository<RiskRecord, Long>`
- `PatternRepository extends JpaRepository<PatternRecord, Long>`
- `SmaRepository extends JpaRepository<SmaRecord, Long>`

**Checkpoint:**
```bash
./mvnw.cmd clean install -DskipTests -pl algorithm-service
# Verificar que Hibernate genera el schema sin errores
```

---

## FASE 6 — Integración (Client + Service + Controller)

**Estado:** pendiente
**Requiere:** Fase 5 completa

### 6a. Endpoint nuevo en ETL service

**Archivo:** `etl-service/src/main/java/com/financial/etl/controller/TransformController.java`

```java
@GetMapping("/cleaned-prices")
public ResponseEntity<Map<String, double[]>> getCleanedPrices()
// Consulta CleanedRecord agrupados por symbol
// Extrae close prices CLEAN/FORWARD_FILLED ordenados por date ASC
// Retorna Map<ticker, double[]>
```

### 6b. EtlServiceClient

**Archivo:** `algorithm-service/.../client/EtlServiceClient.java`
```java
@Component
public class EtlServiceClient {
    // RestTemplate + @Value("${etl.service.url}")
    // GET {etlBaseUrl}/api/etl/transform/cleaned-prices
    // Deserializa ParameterizedTypeReference<Map<String, double[]>>
    public Map<String, double[]> fetchCleanedPrices()
}
```

### 6c. AlgorithmService

```java
@Service
public class AlgorithmService {
    // Inyecta: EtlServiceClient, los 4 módulos, los 4 repositorios
    // runFullAnalysis():
    //   1. fetchCleanedPrices() desde ETL
    //   2. Similitud: toReturns() → todos los pares → persiste SimilarityRecord
    //   3. Riesgo: RiskClassifier.classify() → persiste RiskRecord
    //   4. Patrones: por cada asset → PatternConsecutiveUp + PatternMeanReversion → persiste PatternRecord
    //   5. SMA: por cada asset (ventana configurable 20) → persiste SmaRecord
}
```

### 6d. AlgorithmController

| Método | Endpoint | Descripción | HTTP Status |
|--------|----------|-------------|-------------|
| POST | `/api/algorithm/run` | Dispara análisis completo | 202 Accepted |
| GET | `/api/algorithm/similarity` | Matriz de similitud del último análisis | 200 OK |
| GET | `/api/algorithm/risk` | Ranking de riesgo ordenado | 200 OK |
| GET | `/api/algorithm/patterns/{symbol}` | Patrones de un activo | 200 OK |
| GET | `/api/algorithm/indicators/{symbol}/sma` | SMA con `?window=20` | 200 OK |

**Tests:**
- `AlgorithmServiceTest` — `@ExtendWith(MockitoExtension.class)`, mock client + repositorios
- `AlgorithmControllerTest` — `@WebMvcTest(AlgorithmController.class)`, verifica status codes y JSON

**Checkpoint final:**
```bash
./mvnw.cmd test -pl algorithm-service          # todos en verde
./mvnw.cmd test -pl etl-service                # regresión: ETL sigue funcionando
docker-compose up -d
# POST http://localhost:8083/api/algorithm/run
# GET  http://localhost:8083/api/algorithm/risk
# GET  http://localhost:8083/api/algorithm/similarity
```

---

## Convenciones a seguir (igual que etl-service)

- **Inyección:** siempre por constructor, nunca `@Autowired` en campo
- **DTOs:** builder manual (no Lombok), Jackson-compatible (constructor vacío + getters/setters)
- **Tests de lógica pura:** `new Clase()` en `@BeforeEach`, sin Spring ni Mockito
- **Tests de servicio:** `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
- **Tests de controller:** `@WebMvcTest` + `@MockBean`
- **Javadoc obligatorio** en cada clase: fórmula matemática + complejidad Big O + rango de la salida
