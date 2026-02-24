# Algoritmos del Sistema de Análisis Financiero

> **Proyecto:** Sistema de Análisis de Algoritmos Financieros — Universidad del Quindío
> **Módulo:** `algorithm-service` (puerto 8083)
> **Restricción:** todos los algoritmos están implementados desde sus fundamentos matemáticos en Java puro, sin librerías externas de cálculo estadístico.

---

## Índice

1. [Preprocesamiento: Conversión a Retornos Diarios](#0-preprocesamiento-conversión-a-retornos-diarios)
2. [Media Móvil Simple (SMA)](#1-media-móvil-simple-sma)
3. [Distancia Euclidiana](#2-distancia-euclidiana)
4. [Correlación de Pearson](#3-correlación-de-pearson)
5. [Dynamic Time Warping (DTW)](#4-dynamic-time-warping-dtw)
6. [Similitud Coseno](#5-similitud-coseno)
7. [Ventana Deslizante (Sliding Window)](#6-ventana-deslizante-sliding-window)
8. [Patrón: Días Consecutivos al Alza](#7-patrón-días-consecutivos-al-alza)
9. [Patrón: Reversión a la Media](#8-patrón-reversión-a-la-media)
10. [Calculador de Volatilidad](#9-calculador-de-volatilidad)
11. [Clasificador de Riesgo](#10-clasificador-de-riesgo)
12. [Resumen de Complejidades](#resumen-de-complejidades)

---

## 0. Preprocesamiento: Conversión a Retornos Diarios

Antes de ejecutar cualquier algoritmo de similitud, los precios crudos se transforman en **retornos diarios**. Esta conversión es obligatoria para comparar activos con escalas de precio radicalmente distintas (por ejemplo, una acción en COP a 2.500 frente a otra en USD a 180).

**Fórmula:**

```
r_i = (close_i - close_(i-1)) / close_(i-1)
```

**Ejemplo:**

```
Precios:  [100, 105, 102, 108]
Retornos: [+0.05, -0.0286, +0.0588]
```

**Implementación** — `AlgorithmService.toReturns()`:

```java
private double[] toReturns(double[] prices) {
    double[] returns = new double[prices.length - 1];
    for (int i = 0; i < returns.length; i++) {
        returns[i] = (prices[i + 1] - prices[i]) / prices[i];
    }
    return returns;
}
```

**Nota de alineación:** cuando dos activos tienen distinto número de días de historia (por feriados locales, suspensiones o fecha de inicio diferente), los arrays de retornos se truncan al mínimo común antes de pasarlos a los algoritmos que requieren longitud igual. DTW no necesita alineación previa.

---

## 1. Media Móvil Simple (SMA)

**Clase:** `indicators/SimpleMovingAverage.java`

### Qué es

La SMA suaviza la serie de precios calculando el promedio de los últimos `w` días en cada punto. Elimina el ruido de corto plazo y revela la tendencia subyacente del activo.

### Por qué se usa

Es el indicador técnico más utilizado en análisis bursátil. Se emplea para:

- Detectar cambios de tendencia (cruce de SMA corta y SMA larga).
- Servir de referencia para el patrón de Reversión a la Media.
- Visualización en gráficos candlestick del dashboard.

### Fórmula

```
SMA_t(w) = (1/w) · Σ P_i    para i = t-w+1 ... t
```

Los primeros `w-1` puntos devuelven `Double.NaN` porque no existe historial suficiente para completar la ventana.

### Implementación

La implementación usa **suma deslizante** para lograr O(n) en lugar del O(n·w) de la implementación directa:

```java
// Primera ventana completa: suma de los w primeros precios
double sum = 0.0;
for (int i = 0; i < w; i++) sum += prices[i];
result[w - 1] = sum / w;

// A partir de aquí: suma deslizante — agrega el nuevo, descarta el más antiguo
for (int i = w; i < prices.length; i++) {
    sum += prices[i] - prices[i - w];
    result[i] = sum / w;
}
```

**Ejemplo con w=3:**

```
Precios: [10, 20, 30, 40, 50]
SMA(3):  [NaN, NaN, 20.0, 30.0, 40.0]
          ↑ sin historial    ↑ (10+20+30)/3  ↑ (20+30+40)/3
```

### Complejidades

| | Valor |
|---|---|
| **Temporal** | O(n) — suma deslizante |
| **Espacial** | O(n) — arreglo de salida del mismo tamaño |

### Parámetros de entrada / salida

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `prices` | `double[]` | Precios de cierre en orden cronológico |
| `w` | `int` | Tamaño de la ventana (≥ 1) |
| **Retorna** | `double[]` | Misma longitud que `prices`; primeros `w-1` son `NaN` |

---

## 2. Distancia Euclidiana

**Clase:** `similarity/EuclideanDistance.java`

### Qué es

La distancia euclidiana mide la diferencia punto a punto entre dos series de retornos. Es la generalización n-dimensional de la distancia entre dos puntos en el plano.

### Por qué se usa

Es la métrica de similitud más intuitiva. Cuanto menor sea su valor, más se parecen las series en magnitud y forma. Un valor cercano a cero indica activos que se comportan casi idénticamente día a día.

**Caso de uso típico:** detectar fondos ETF que replican el mismo índice (VOO y SPY sobre el S&P 500 tendrán distancia euclidiana casi nula).

### Fórmula

```
d(a, b) = sqrt( Σ (a_i - b_i)² )    para i = 0 ... n-1
```

### Implementación

```java
double sum = 0.0;
for (int i = 0; i < a.length; i++) {
    double diff = a[i] - b[i];
    sum += diff * diff;
}
return Math.sqrt(sum);
```

**Ejemplo:**

```
a = [0.01, -0.02, 0.03]
b = [0.01, -0.02, 0.03]
→ d = 0.0  (series idénticas)

a = [0.0, 0.0]
b = [3.0, 4.0]
→ d = sqrt(9 + 16) = 5.0
```

### Edge cases

| Situación | Comportamiento |
|-----------|---------------|
| Arrays de distinta longitud | Lanza `IllegalArgumentException` |
| Arrays vacíos | Devuelve `0.0` |
| Series idénticas | Devuelve `0.0` |

### Complejidades

| | Valor |
|---|---|
| **Temporal** | O(n) |
| **Espacial** | O(1) — sin estructuras auxiliares |

### Parámetros de entrada / salida

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `a` | `double[]` | Primera serie de retornos diarios |
| `b` | `double[]` | Segunda serie de retornos diarios |
| **Retorna** | `double` | Distancia ≥ 0 |

---

## 3. Correlación de Pearson

**Clase:** `similarity/PearsonCorrelation.java`

### Qué es

La correlación de Pearson mide la fuerza y dirección de la relación lineal entre dos series. Es el estándar estadístico para evaluar si dos activos se mueven juntos, en sentidos opuestos, o de forma independiente.

### Por qué se usa

Es la métrica fundamental para **análisis de diversificación de portafolios**. Según la teoría moderna de portafolios (Markowitz), combinar activos con baja correlación entre sí reduce el riesgo total sin sacrificar el retorno esperado.

**Interpretación:**

| Valor | Significado |
|-------|------------|
| r ≈ +1 | Se mueven juntos — sin diversificación |
| r ≈  0 | Independientes — diversificación perfecta |
| r ≈ −1 | Se mueven opuestos — cobertura natural |

### Fórmula

```
r = Σ[(x_i - x̄)(y_i - ȳ)] / sqrt( Σ(x_i - x̄)² · Σ(y_i - ȳ)² )
```

Donde `x̄` e `ȳ` son las medias de cada serie.

### Implementación

Se realiza en **dos pasadas** sobre los datos para evitar almacenar desviaciones intermedias:

```java
// Pasada 1: calcular medias
double sumX = 0.0, sumY = 0.0;
for (int i = 0; i < x.length; i++) { sumX += x[i]; sumY += y[i]; }
double meanX = sumX / x.length;
double meanY = sumY / y.length;

// Pasada 2: calcular numerador y denominadores simultáneamente
double numerator = 0.0, denomX = 0.0, denomY = 0.0;
for (int i = 0; i < x.length; i++) {
    double dx = x[i] - meanX;
    double dy = y[i] - meanY;
    numerator += dx * dy;
    denomX    += dx * dx;
    denomY    += dy * dy;
}
return numerator / Math.sqrt(denomX * denomY);
```

### Edge cases

| Situación | Comportamiento |
|-----------|---------------|
| Serie constante (σ = 0) | Devuelve `Double.NaN` (denominador = 0) |
| Arrays de distinta longitud | Lanza `IllegalArgumentException` |
| Arrays vacíos | Lanza `IllegalArgumentException` |

### Complejidades

| | Valor |
|---|---|
| **Temporal** | O(n) — dos recorridos lineales |
| **Espacial** | O(1) — sin estructuras auxiliares |

### Parámetros de entrada / salida

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `x` | `double[]` | Primera serie de retornos diarios |
| `y` | `double[]` | Segunda serie de retornos diarios |
| **Retorna** | `double` | Correlación en [−1, 1], o `NaN` si σ = 0 |

---

## 4. Dynamic Time Warping (DTW)

**Clase:** `similarity/DynamicTimeWarping.java`

### Qué es

DTW es un algoritmo de programación dinámica que mide la similitud entre dos series temporales permitiendo **desfases temporales**. A diferencia de la distancia euclidiana, no compara los puntos en la misma posición sino que busca la alineación óptima entre ambas series.

### Por qué se usa

La distancia euclidiana penaliza series que se mueven igual pero con un desfase de uno o varios días. DTW detecta este caso correctamente. Es esencial para identificar **activos seguidores** que reaccionan tarde a movimientos de activos líderes (por ejemplo, empresas pequeñas que replican con retraso el movimiento de las grandes del mismo sector).

**Ejemplo que motiva DTW:**

```
A: [0, 1, 2, 3]     (sube primero)
B: [0, 0, 1, 2, 3]  (replica con un día de retraso)

Euclidean: alta (compara pos a pos, ve diferencias)
DTW:       baja  (encuentra la alineación óptima con el desfase)
```

### Fórmula — Programación Dinámica

Se construye una matriz de costos acumulados `M[n][m]`:

```
M[0][0] = |a_0 - b_0|

M[i][0] = |a_i - b_0| + M[i-1][0]          (primera columna)
M[0][j] = |a_0 - b_j| + M[0][j-1]          (primera fila)

M[i][j] = |a_i - b_j| + min(M[i-1][j],     (viene de arriba)
                             M[i][j-1],      (viene de la izquierda)
                             M[i-1][j-1])    (viene en diagonal)

Resultado: M[n-1][m-1]
```

### Implementación

```java
double[][] dtw = new double[n][m];

// Inicializar con infinito
for (int i = 0; i < n; i++)
    Arrays.fill(dtw[i], Double.MAX_VALUE);

dtw[0][0] = Math.abs(a[0] - b[0]);

// Bordes: solo un camino posible
for (int i = 1; i < n; i++) dtw[i][0] = Math.abs(a[i] - b[0]) + dtw[i-1][0];
for (int j = 1; j < m; j++) dtw[0][j] = Math.abs(a[0] - b[j]) + dtw[0][j-1];

// Relleno general
for (int i = 1; i < n; i++)
    for (int j = 1; j < m; j++) {
        double cost = Math.abs(a[i] - b[j]);
        dtw[i][j]   = cost + min(dtw[i-1][j], dtw[i][j-1], dtw[i-1][j-1]);
    }

return dtw[n-1][m-1];
```

### Nota de rendimiento

DTW es el algoritmo **más costoso** del sistema: O(n²) para series de igual longitud. Con un portafolio de 20 activos se calculan C(20,2) = **190 pares**, cada uno sobre ~1240 puntos de retornos. El tiempo de ejecución de `/run` está dominado por este cálculo.

### Complejidades

| | Valor |
|---|---|
| **Temporal** | O(n × m) |
| **Espacial** | O(n × m) — la matriz completa reside en memoria |

### Parámetros de entrada / salida

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `a` | `double[]` | Primera serie (puede tener longitud distinta a `b`) |
| `b` | `double[]` | Segunda serie |
| **Retorna** | `double` | Costo acumulado de alineación óptima ≥ 0 |

---

## 5. Similitud Coseno

**Clase:** `similarity/CosineSimilarity.java`

### Qué es

La similitud coseno mide el **ángulo** entre dos vectores de retornos, ignorando completamente su magnitud. Dos activos pueden tener escalas de precio radicalmente distintas pero moverse siempre en la misma dirección; esta métrica captura exactamente eso.

### Por qué se usa

Complementa a Pearson para comparar activos de mercados distintos (acciones colombianas en COP vs acciones americanas en USD). Mientras Pearson es sensible a la forma de la distribución, Coseno solo evalúa la dirección del vector de retornos.

**Interpretación:**

| Valor | Significado |
|-------|------------|
| S ≈ +1 | Misma dirección de movimiento en todos los días |
| S ≈  0 | Movimientos ortogonales (sin relación direccional) |
| S ≈ −1 | Movimientos exactamente opuestos |

### Fórmula

```
S_C(x, y) = (x · y) / (|x| · |y|)

Donde:
  x · y = Σ x_i · y_i          (producto punto)
  |x|   = sqrt(Σ x_i²)         (norma euclidiana del vector x)
  |y|   = sqrt(Σ y_i²)         (norma euclidiana del vector y)
```

### Implementación

```java
double dotProduct = 0.0, normX = 0.0, normY = 0.0;
for (int i = 0; i < x.length; i++) {
    dotProduct += x[i] * y[i];
    normX      += x[i] * x[i];
    normY      += y[i] * y[i];
}
double denominator = Math.sqrt(normX) * Math.sqrt(normY);
if (denominator == 0.0) return Double.NaN;
return dotProduct / denominator;
```

**Ejemplo:**

```
x = [1.0, 2.0, 3.0]
y = [2.0, 4.0, 6.0]   (y = 2·x, misma dirección)
→ S_C = 1.0

x = [1.0,  0.0]
y = [0.0,  1.0]   (ortogonales)
→ S_C = 0.0
```

### Edge cases

| Situación | Comportamiento |
|-----------|---------------|
| Vector cero | Devuelve `Double.NaN` |
| Arrays de distinta longitud | Lanza `IllegalArgumentException` |
| Arrays vacíos | Devuelve `Double.NaN` |

### Complejidades

| | Valor |
|---|---|
| **Temporal** | O(n) — un único recorrido |
| **Espacial** | O(1) — sin estructuras auxiliares |

### Parámetros de entrada / salida

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `x` | `double[]` | Primera serie de retornos diarios |
| `y` | `double[]` | Segunda serie de retornos diarios |
| **Retorna** | `double` | Similitud en [−1, 1], o `NaN` si algún vector es cero |

---

## 6. Ventana Deslizante (Sliding Window)

**Clase:** `patterns/SlidingWindow.java`

### Qué es

La ventana deslizante es una utilidad que recorre una serie temporal extrayendo todas las sub-series posibles de tamaño `k`. Es la estructura base sobre la que se construyen los detectores de patrones.

### Por qué se usa

Permite evaluar condiciones locales sobre toda la historia de un activo de forma sistemática. En lugar de analizar la serie completa de una vez, se evalúa cada segmento de `k` días de manera independiente.

### Mecánica

Para una serie de longitud `n` y ventana `k`, produce `n - k + 1` sub-series:

```
Serie:    [P0, P1, P2, P3, P4]   n=5, k=3
Ventanas: [P0,P1,P2]  inicio=0
          [P1,P2,P3]  inicio=1
          [P2,P3,P4]  inicio=2
Total: 5 - 3 + 1 = 3 ventanas
```

### Implementación

```java
public List<WindowSlice> extract(double[] series, int k) {
    List<WindowSlice> windows = new ArrayList<>();
    if (k > series.length || k <= 0) return windows;
    for (int i = 0; i <= series.length - k; i++) {
        windows.add(new WindowSlice(i, Arrays.copyOfRange(series, i, i + k)));
    }
    return windows;
}
```

Cada `WindowSlice` es un record Java con `startIndex` (posición en la serie original) y `values` (copia de los valores de la ventana).

### Complejidades

| | Valor |
|---|---|
| **Temporal** | O(n) — un recorrido lineal |
| **Espacial** | O(k) por ventana producida |

### Parámetros de entrada / salida

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `series` | `double[]` | Serie de valores en orden cronológico |
| `k` | `int` | Tamaño de la ventana |
| **Retorna** | `List<WindowSlice>` | Lista vacía si `k > n` o `k ≤ 0` |

---

## 7. Patrón: Días Consecutivos al Alza

**Clase:** `patterns/PatternConsecutiveUp.java`

### Qué es

Detecta secuencias de exactamente `k` días consecutivos donde el retorno diario fue positivo en todos los días de la ventana. Mide la persistencia del momentum alcista de un activo.

### Por qué se usa

El análisis de frecuencia de este patrón responde: *¿con qué probabilidad histórica un activo sostuvo una racha alcista de k días?* Un activo con alta frecuencia relativa de este patrón tiene mayor persistencia de momentum, lo que es útil para estrategias de seguimiento de tendencia.

### Definición formal

```
El patrón se cumple en la ventana [t, t+k] si:
  ∀ i ∈ [1, k] : retorno_i > 0

Donde:
  retorno_i = (close_i - close_(i-1)) / close_(i-1)
```

### Implementación

Usa `SlidingWindow` con ventana de tamaño `k+1` (necesita `k+1` precios para calcular `k` retornos):

```java
List<WindowSlice> windows = slidingWindow.extract(closePrices, k + 1);

for (WindowSlice window : windows) {
    double[] prices = window.values();
    boolean allUp = true;
    for (int i = 1; i < prices.length; i++) {
        double ret = (prices[i] - prices[i-1]) / prices[i-1];
        if (ret <= 0) { allUp = false; break; }   // early exit
    }
    if (allUp) count++;
}

relativeFrequency = (double) count / windows.size();
```

**Ejemplo (k=2):**

```
Precios:  [10, 11, 12, 11, 13]
Ventanas de 3: [10,11,12] → retornos [+,+] ✓
               [11,12,11] → retornos [+,-] ✗
               [12,11,13] → retornos [-,+] ✗
               [11,13]  → fuera de rango

count=1, totalWindows=3, relativeFrequency=0.333
```

### Complejidades

| | Valor |
|---|---|
| **Temporal** | O(n) — recorrido lineal con early exit |
| **Espacial** | O(n) — lista de ventanas producidas por SlidingWindow |

### Parámetros de entrada / salida

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `closePrices` | `double[]` | Precios de cierre en orden cronológico |
| `k` | `int` | Número de días consecutivos al alza requeridos |
| **Retorna** | `PatternResult` | Conteo, frecuencia relativa e índices de inicio de cada ocurrencia |

---

## 8. Patrón: Reversión a la Media

**Clase:** `patterns/PatternMeanReversion.java`

### Qué es

Detecta momentos en los que el precio de cierre se aleja excesivamente de su promedio reciente, medido en términos de desviaciones estándar locales. Indica sobre-extensión del precio y potencial corrección inminente.

### Por qué se usa

Es la base de las estrategias **contrarian**: cuando un precio se aleja demasiado de su media histórica reciente, estadísticamente tiende a revertir. El sistema calcula cuántas veces ocurrió esa situación en 5 años de historia, dando evidencia empírica de la fortaleza del patrón en cada activo.

### Definición formal

```
El patrón se detecta en el punto t si:
  |close_t - SMA_w(t)| > threshold · σ_w(t)

Donde:
  SMA_w(t) = media de los últimos w precios hasta t
  σ_w(t)   = desviación estándar de los últimos w precios hasta t
  threshold = número de σ que define "sobre-extensión" (por defecto 2.0)

Dirección:
  close_t > SMA_w(t) → ABOVE  (precio sobre-extendido al alza)
  close_t < SMA_w(t) → BELOW  (precio sobre-extendido a la baja)
```

### Implementación

Depende de `SimpleMovingAverage` para calcular los valores SMA, luego calcula σ local por ventana:

```java
double[] smaValues = sma.compute(closePrices, w);

for (int t = w - 1; t < closePrices.length; t++) {
    double smaT = smaValues[t];

    // Desviación estándar poblacional de los w precios hasta t
    double sumSq = 0.0;
    for (int i = t - w + 1; i <= t; i++) {
        double diff = closePrices[i] - smaT;
        sumSq += diff * diff;
    }
    double sigma = Math.sqrt(sumSq / w);

    if (sigma > 0 && Math.abs(closePrices[t] - smaT) > threshold * sigma) {
        // señal detectada
    }
}
```

**Nota matemática:** con un solo outlier en una ventana de tamaño `w`, ese outlier siempre se sitúa exactamente en `sqrt(w-1)` desviaciones estándar de la media (identidad algebraica). Para ventana `w=5`, el outlier único está siempre en exactamente 2σ. Por eso el sistema usa threshold=2.0 y necesita variación real en la ventana para detectar señales.

### Complejidades

| | Valor |
|---|---|
| **Temporal** | O(n · w) — bucle externo n iteraciones, interno w iteraciones |
| **Espacial** | O(n) — array de valores SMA |

### Parámetros de entrada / salida

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `closePrices` | `double[]` | Precios de cierre en orden cronológico |
| `w` | `int` | Tamaño de ventana para SMA y σ local |
| `threshold` | `double` | Número de σ para considerar sobre-extensión (default 2.0) |
| **Retorna** | `PatternResult` | Conteo, frecuencia relativa y detalle de cada señal (precio, SMA, σ, dirección) |

---

## 9. Calculador de Volatilidad

**Clase:** `risk/VolatilityCalculator.java`

### Qué es

Calcula la **volatilidad histórica anualizada** de un activo: la intensidad de las fluctuaciones de precio expresada como la desviación estándar de sus retornos logarítmicos diarios, escalada a un año de trading.

### Por qué se usa

Es la métrica cuantitativa más importante del análisis de riesgo. A diferencia de los retornos aritméticos, los **retornos logarítmicos** son aditivos en el tiempo (propiedades matemáticas superiores para modelado financiero) y se distribuyen de forma más simétrica. La anualización con factor `sqrt(252)` permite comparar activos independientemente del número de días de historia disponibles.

### Fórmulas

```
1. Retorno logarítmico:
   r_i = ln(close_i / close_(i-1))

2. Media de retornos:
   r̄ = (1/n) · Σ r_i

3. Desviación estándar diaria (poblacional):
   σ_diaria = sqrt( (1/n) · Σ(r_i - r̄)² )

4. Volatilidad anualizada:
   σ_anual = σ_diaria · sqrt(252)
```

El factor `252` es el número estándar de días hábiles bursátiles en un año calendario.

### Implementación

```java
int n = closePrices.length - 1;
double[] logReturns = new double[n];

// Paso 1: retornos logarítmicos y su suma
double sumReturns = 0.0;
for (int i = 0; i < n; i++) {
    logReturns[i] = Math.log(closePrices[i + 1] / closePrices[i]);
    sumReturns += logReturns[i];
}
double meanReturn = sumReturns / n;

// Paso 2: varianza poblacional
double sumSq = 0.0;
for (int i = 0; i < n; i++) {
    double diff = logReturns[i] - meanReturn;
    sumSq += diff * diff;
}
double dailySigma      = Math.sqrt(sumSq / n);
double annualizedSigma = dailySigma * Math.sqrt(252);
```

**Ejemplo:**

```
Precios constantes [100, 100, 100, 100]:
  retornos log = [ln(1), ln(1), ln(1)] = [0, 0, 0]
  σ_diaria = 0.0
  σ_anual  = 0.0  → activo completamente estable

Precios alternando [100, 110, 100, 110]:
  retornos log ≈ [+0.0953, -0.0953, +0.0953]
  σ_diaria ≈ 0.0953
  σ_anual  ≈ 0.0953 · 15.87 ≈ 1.512 (151% volatilidad anual)
```

### Edge cases

| Situación | Comportamiento |
|-----------|---------------|
| Menos de 2 precios | Lanza `IllegalArgumentException` |
| Precios constantes | Devuelve σ = 0.0 correctamente |

### Complejidades

| | Valor |
|---|---|
| **Temporal** | O(n) — dos recorridos lineales |
| **Espacial** | O(n) — almacena el array de retornos logarítmicos |

### Parámetros de entrada / salida

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `closePrices` | `double[]` | Precios de cierre en orden cronológico (mínimo 2) |
| **Retorna** | `VolatilityResult` | `logReturns[]`, `dailySigma`, `annualizedSigma` |

---

## 10. Clasificador de Riesgo

**Clase:** `risk/RiskClassifier.java`

### Qué es

Toma todos los activos del portafolio, calcula su volatilidad anualizada mediante `VolatilityCalculator`, los categoriza en tres perfiles de riesgo y devuelve el resultado ordenado de menor a mayor volatilidad.

### Por qué se usa

Permite construir portafolios adaptados al perfil de cada inversor. Un cliente conservador solo debería tener activos CONSERVATIVE; uno agresivo puede incluir activos de los tres niveles. El ranking ordenado facilita la selección de activos para rebalanceo de portafolios.

### Criterios de clasificación

```
σ_anual < 0.15               → CONSERVATIVE  (< 15% volatilidad anual)
0.15 ≤ σ_anual ≤ 0.30       → MODERATE      (15% – 30%)
σ_anual > 0.30               → AGGRESSIVE    (> 30%)
```

Estos umbrales son estándar en la industria de gestión de activos.

### Implementación

```java
for (Map.Entry<String, double[]> entry : portfolio.entrySet()) {
    VolatilityResult vol = volatilityCalculator.compute(entry.getValue());
    double sigma = vol.getAnnualizedSigma();

    RiskCategory category;
    if      (sigma < 0.15) category = CONSERVATIVE;
    else if (sigma <= 0.30) category = MODERATE;
    else                    category = AGGRESSIVE;

    profiles.add(RiskProfile.builder()
        .ticker(entry.getKey())
        .annualizedVolatility(sigma)
        .category(category)
        .build());
}

// Ordenar ascendente por volatilidad
profiles.sort(Comparator.comparingDouble(RiskProfile::getAnnualizedVolatility));
```

**Ejemplo de salida ordenada:**

```
[
  { ticker: "GLD",  σ_anual: 0.11, category: CONSERVATIVE },
  { ticker: "VOO",  σ_anual: 0.17, category: MODERATE     },
  { ticker: "AAPL", σ_anual: 0.24, category: MODERATE     },
  { ticker: "TSLA", σ_anual: 0.55, category: AGGRESSIVE   }
]
```

### Complejidades

| | Valor |
|---|---|
| **Temporal** | O(A · n) — A activos × n días cada uno |
| **Espacial** | O(A) — una entrada en la lista por activo |

### Parámetros de entrada / salida

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `portfolio` | `Map<String, double[]>` | Ticker → array de precios de cierre |
| **Retorna** | `List<RiskProfile>` | Lista ordenada ASC por `annualizedVolatility` |

---

## Resumen de Complejidades

| Algoritmo | Clase | Temporal | Espacial |
|-----------|-------|----------|----------|
| Media Móvil Simple | `SimpleMovingAverage` | O(n) | O(n) |
| Distancia Euclidiana | `EuclideanDistance` | O(n) | O(1) |
| Correlación de Pearson | `PearsonCorrelation` | O(n) | O(1) |
| Dynamic Time Warping | `DynamicTimeWarping` | O(n × m) | O(n × m) |
| Similitud Coseno | `CosineSimilarity` | O(n) | O(1) |
| Ventana Deslizante | `SlidingWindow` | O(n) | O(k) por ventana |
| Días Consecutivos al Alza | `PatternConsecutiveUp` | O(n) | O(n) |
| Reversión a la Media | `PatternMeanReversion` | O(n · w) | O(n) |
| Volatilidad Histórica | `VolatilityCalculator` | O(n) | O(n) |
| Clasificador de Riesgo | `RiskClassifier` | O(A · n) | O(A) |

**Variables:**
- `n` — número de días de historia del activo (≈ 1240 en producción)
- `m` — número de días de historia del segundo activo en comparación DTW
- `w` — tamaño de la ventana SMA o de detección de patrones
- `k` — tamaño de la ventana de días consecutivos
- `A` — número de activos en el portafolio (20 en producción)

**Algoritmo dominante en tiempo de ejecución:** DTW con O(n × m) sobre 190 pares de activos.

---

## Árbol de dependencias entre algoritmos

```
SimpleMovingAverage
        │
        ├──► PatternMeanReversion  (usa SMA internamente)
        │
SlidingWindow
        │
        └──► PatternConsecutiveUp  (usa SlidingWindow internamente)

VolatilityCalculator
        │
        └──► RiskClassifier        (llama a VolatilityCalculator por cada activo)

EuclideanDistance  ─┐
PearsonCorrelation ─┤── independientes entre sí, operan sobre retornos diarios
DynamicTimeWarping ─┤
CosineSimilarity   ─┘
```
