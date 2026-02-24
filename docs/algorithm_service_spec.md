# 📐 Epic: Algorithm Service — Especificación de Requerimientos

> **Proyecto:** Sistema de Análisis de Algoritmos Financieros — Universidad del Quindío  
> **Restricción crítica:** Todos los algoritmos deben implementarse **manualmente desde sus fundamentos matemáticos** en Java. Está **prohibido** usar librerías externas que encapsulen la lógica de cálculo como Apache Commons Math, Weka, o cualquier función equivalente de alto nivel.

---

## 🗂️ Estructura del Algorithm Service

El servicio se organiza en 4 paquetes dentro de `com.financiero.algorithms`:

```
similarity/     → 4 algoritmos de comparación entre series de tiempo
patterns/       → detección de patrones recurrentes con ventana deslizante
risk/           → cálculo de volatilidad y clasificación de riesgo
indicators/     → cálculo de indicadores técnicos (SMA)
```

---

## Módulo 1: Algoritmos de Similitud

Todos los algoritmos de este módulo reciben como entrada dos series de tiempo (arreglos de `double`) y retornan un valor numérico que representa qué tan similares son. Deben operar preferiblemente sobre **retornos diarios** y no sobre precios crudos, para hacer comparables activos con escalas muy distintas (ej. precios en COP vs USD).

Cada clase debe documentar en su Javadoc:
- La fórmula matemática que implementa
- Su complejidad temporal y espacial en notación Big O
- Qué significa el valor retornado y cuál es su rango

---

### 1.1 — Distancia Euclidiana

**¿Qué mide?**  
La diferencia absoluta punto a punto entre dos series. Cuanto menor sea el valor retornado, más similares son las series en magnitud.

**Fórmula:**
```
d(p, q) = sqrt( Σ (p_i - q_i)² )   para i = 1 ... n
```

**Entradas:** Dos arreglos de `double` de igual longitud.  
**Salida:** Un `double` ≥ 0. El valor 0 indica series idénticas.  
**Complejidad:** `O(n)` temporal, `O(1)` espacial.

---

### 1.2 — Correlación de Pearson

**¿Qué mide?**  
Si dos activos se mueven en la misma dirección (positiva), en direcciones opuestas (negativa) o de forma independiente (cero). Es el estándar para evaluar diversificación de portafolios.

**Fórmula:**
```
r = Σ[(x_i - x̄)(y_i - ȳ)] / sqrt( Σ(x_i - x̄)² · Σ(y_i - ȳ)² )
```

**Entradas:** Dos arreglos de `double` de igual longitud (retornos diarios).  
**Salida:** Un `double` en el rango `[-1, 1]`.  
**Complejidad:** `O(n)` temporal, `O(1)` espacial.

---

### 1.3 — Dynamic Time Warping (DTW)

**¿Qué mide?**  
La similitud entre dos series que pueden tener desfases temporales. A diferencia de la distancia euclidiana, no compara los puntos en la misma posición sino que busca la alineación óptima entre ambas series. Es esencial para detectar activos "seguidores" que reaccionan tarde a movimientos de activos "líderes".

**Fórmula:**  
Se construye una matriz de costos acumulados donde cada celda `M[i][j]` representa el costo mínimo de alinear los primeros `i` elementos de la serie A con los primeros `j` de la serie B:
```
M[i][j] = dist(a_i, b_j) + min(M[i-1][j], M[i][j-1], M[i-1][j-1])
```

**Entradas:** Dos arreglos de `double` (pueden tener longitud diferente).  
**Salida:** Un `double` ≥ 0. El valor de la celda `M[n][m]` de la matriz final.  
**Complejidad:** `O(n × m)` temporal y espacial.

> ⚡ Este es el algoritmo computacionalmente más costoso. Para un portafolio de 20 activos existen 190 pares únicos de comparación. Considerar procesamiento asíncrono o caché de resultados.

---

### 1.4 — Similitud por Coseno

**¿Qué mide?**  
La dirección del movimiento entre dos vectores de retornos, ignorando la magnitud. Dos activos pueden tener escalas de precio radicalmente distintas pero moverse en la misma dirección; esta métrica captura exactamente eso.

**Fórmula:**
```
S_C(x, y) = (x · y) / (|x| · |y|)

Donde:
  x · y  = Σ x_i · y_i         (producto punto)
  |x|    = sqrt(Σ x_i²)        (norma euclidiana)
```

**Entradas:** Dos arreglos de `double` de igual longitud (retornos diarios).  
**Salida:** Un `double` en el rango `[-1, 1]`. Cercano a 1 = misma dirección de movimiento.  
**Complejidad:** `O(n)` temporal, `O(1)` espacial.

---

## Módulo 2: Detección de Patrones

### 2.1 — Ventana Deslizante (Sliding Window)

**¿Qué hace?**  
Recorre toda la serie de tiempo desplazando una sub-ventana de tamaño `k` de izquierda a derecha, produciendo todas las sub-series posibles de esa longitud. Es la utilidad base que deben reutilizar los detectores de patrones.

**Entradas:** Una serie de `double` y un entero `k` (tamaño de la ventana).  
**Salida:** Una colección de sub-series de tamaño `k`, cada una con su índice de inicio en la serie original.  
**Complejidad:** `O(n)` temporal, `O(k)` espacial por ventana.

---

### 2.2 — Patrón A: Días Consecutivos al Alza

**¿Qué detecta?**  
Secuencias de `k` días donde el retorno diario fue positivo en todos los días de la ventana, es decir, periodos sostenidos de momentum alcista.

**Definición formal:**
```
El patrón se cumple si: ∀ i ∈ [1, k] : retorno_i > 0
Donde retorno_i = (close_i - close_(i-1)) / close_(i-1)
```

**Entradas:** Arreglo de precios de cierre y entero `k`.  
**Salida:** Número total de ocurrencias, frecuencia relativa (ocurrencias / total de ventanas) y lista de índices donde inicia cada ocurrencia.  
**Complejidad:** `O(n)` temporal.

---

### 2.3 — Patrón B: Reversión a la Media

**¿Qué detecta?**  
Momentos en los que el precio de cierre se aleja excesivamente de su promedio reciente, específicamente más de 2 desviaciones estándar respecto a la Media Móvil Simple de una ventana de `w` días. Indica sobre-extensión del precio y potencial corrección inminente.

**Definición formal:**
```
El patrón se cumple en el punto t si:
|close_t - SMA_w(t)| > 2 · σ_w(t)

Donde σ_w(t) es la desviación estándar de los últimos w precios hasta t.
```

**Entradas:** Arreglo de precios de cierre, entero `w` (ventana) y `threshold` (por defecto 2.0).  
**Salida:** Número de ocurrencias, frecuencia relativa y detalle de cada señal detectada (índice, precio, SMA, sigma y si fue por encima o por debajo de la media).  
**Complejidad:** `O(n · w)` temporal.

---

## Módulo 3: Volatilidad y Clasificación de Riesgo

### 3.1 — Calculador de Volatilidad Histórica

**¿Qué calcula?**  
La intensidad de las fluctuaciones de precio de un activo, representada como la desviación estándar de sus retornos logarítmicos diarios, anualizada. Es la métrica cuantitativa que fundamenta toda clasificación de riesgo.

**Fórmulas:**
```
Retorno logarítmico: r_i = ln(close_i / close_(i-1))

Desviación estándar diaria: σ = sqrt( (1/n) · Σ(r_i - r̄)² )

Volatilidad anualizada: σ_anual = σ · sqrt(252)
```
> Se usa `252` porque es el número estándar de días hábiles bursátiles en un año.

**Entradas:** Arreglo de precios de cierre (mínimo 2 elementos).  
**Salida:** Los retornos logarítmicos calculados, la sigma diaria y la sigma anualizada.  
**Complejidad:** `O(n)` temporal, `O(n)` espacial.

---

### 3.2 — Clasificador de Riesgo

**¿Qué hace?**  
Toma todos los activos del portafolio, calcula su volatilidad anualizada y los categoriza en un perfil de riesgo. El resultado es un ranking ordenado de menor a mayor riesgo de los 20+ activos.

**Criterios de clasificación:**

| Categoría   | Volatilidad Anualizada |
|-------------|------------------------|
| Conservador | σ < 15%               |
| Moderado    | 15% ≤ σ ≤ 30%        |
| Agresivo    | σ > 30%               |

**Entradas:** Un `Map<String, double[]>` donde la clave es el ticker del activo y el valor es su arreglo de precios de cierre.  
**Salida:** Lista de activos ordenada ascendentemente por volatilidad, cada uno con su ticker, volatilidad calculada y categoría asignada.  
**Complejidad:** `O(A · n)` donde `A` es el número de activos y `n` el número de días históricos.

---

## Módulo 4: Indicadores Técnicos

### 4.1 — Media Móvil Simple (SMA)

**¿Qué calcula?**  
El promedio de los últimos `w` precios de cierre para cada punto de la serie. Suaviza la tendencia del precio eliminando el ruido de corto plazo. Es un componente obligatorio en los gráficos candlestick del dashboard.

**Fórmula:**
```
SMA_t(w) = (1/w) · Σ P_i    para i = t-w+1 ... t
```

**Entradas:** Arreglo de precios de cierre y entero `w` (tamaño de la ventana).  
**Salida:** Arreglo de igual longitud que la entrada. Los primeros `w-1` valores deben ser `Double.NaN` porque no existe suficiente historial para calcular la ventana completa.  
**Complejidades:**  
- Implementación directa: `O(n · w)` temporal.  
- Implementación optimizada con suma deslizante: `O(n)` temporal. Recomendada para ventanas grandes (SMA 50 y SMA 200).

---

## 📊 Resumen de Complejidades

| Algoritmo                  | Clase Java               | Temporal    | Espacial    |
|----------------------------|--------------------------|-------------|-------------|
| Distancia Euclidiana       | `EuclideanDistance`      | O(n)        | O(1)        |
| Correlación de Pearson     | `PearsonCorrelation`     | O(n)        | O(1)        |
| Dynamic Time Warping       | `DynamicTimeWarping`     | O(n × m)    | O(n × m)    |
| Similitud por Coseno       | `CosineSimilarity`       | O(n)        | O(1)        |
| Sliding Window             | `SlidingWindow`          | O(n)        | O(k)        |
| Patrón A — Consecutivos    | `PatternConsecutiveUp`   | O(n)        | O(n)        |
| Patrón B — Mean Reversion  | `PatternMeanReversion`   | O(n · w)    | O(n)        |
| Volatilidad Histórica      | `VolatilityCalculator`   | O(n)        | O(n)        |
| Clasificador de Riesgo     | `RiskClassifier`         | O(A · n)    | O(A)        |
| SMA directa                | `SimpleMovingAverage`    | O(n · w)    | O(n)        |
| SMA optimizada             | `SimpleMovingAverage`    | O(n)        | O(n)        |

---

## 🔗 Contrato de Integración con el Módulo ETL

Todos los algoritmos deben consumir la estructura de datos producida por el módulo ETL:

```
Map<String, double[]>
  Clave  → ticker del activo (ej: "ECOPETROL", "VOO")
  Valor  → arreglo de precios de cierre en orden cronológico (≥ 1250 puntos)
```

Los algoritmos de similitud deben transformar los precios a retornos diarios antes de comparar, para garantizar que activos con escalas de precio muy distintas sean comparables entre sí.

---

## ✅ Checklist de Implementación

- [ ] Cada clase documenta en Javadoc su fórmula matemática y complejidad Big O
- [ ] Ningún algoritmo usa librerías externas de cálculo estadístico o de similitud
- [ ] Todos manejan edge cases: arreglos vacíos, longitudes distintas, divisiones por cero
- [ ] Los algoritmos de similitud operan sobre retornos y no sobre precios crudos
- [ ] Los detectores de patrones retornan frecuencia relativa además del conteo absoluto
- [ ] Los detectores de patrones retornan los índices de ocurrencia para visualización
- [ ] La volatilidad usa retornos logarítmicos (`Math.log`) y factor de anualización `sqrt(252)`
- [ ] La SMA retorna `Double.NaN` en los primeros `w-1` puntos
- [ ] El clasificador de riesgo produce un ranking ordenado de los 20+ activos
