# Epic: Report Service — Guía de Desarrollo para Claude Code

## Contexto del Proyecto

Este proyecto es un **sistema de análisis cuantitativo de mercados financieros** construido con arquitectura de microservicios en **Java / Spring Boot 3**. Ya existen tres microservicios funcionales que este servicio consume:

- **ETL Service (`:8081`)** → Provee los datos históricos OHLCV limpios y unificados del dataset maestro (≥20 activos, 5 años).
- **Algorithm Service (`:8082`)** → Provee los resultados de los algoritmos de similitud (Euclidiana, Pearson, DTW, Coseno), volatilidad histórica, patrones detectados y clasificaciones de riesgo calculadas.
- **Gateway Service (`:8080`)** → Punto único de entrada; enruta al Report Service en el path `/api/v1/reports/**`.

El **Report Service (`:8083`)** es la **capa de síntesis y entrega**: toma los datos y resultados algorítmicos ya calculados, los consolida, los expone de forma estructurada y genera los reportes exportables que el frontend y el evaluador necesitan.

---

## Estado Actual del Servicio

El módulo `report-service` existe en el proyecto pero está **prácticamente vacío**:

```
report-service/
  └── src/
      ├── main/java/com/financial/report/
      │   └── ReportServiceApplication.java   ← solo el main
      └── main/resources/
          └── application.properties           ← configuración mínima
      └── test/java/com/financial/report/
          └── ReportServiceApplicationTest.java
```

Toda la lógica de negocio de esta epic está **por construir desde cero** dentro de este módulo.

---

## Historias de Usuario que Cubre esta Epic

| ID | Historia | Story Points |
|----|----------|:---:|
| **US-20** | Matriz de Correlación — calcular y almacenar la matriz completa del portafolio | 5 |
| **US-23** | Generación de Reporte PDF — exportar un documento auditable con todos los hallazgos | 8 |
| **US-24** | API REST Reports — endpoints de consulta para que el frontend y el gateway consuman | 3 |

> ✅ **US-21 (Clasificación de Riesgo) y US-22 ya fueron completadas.**

**Total pendiente: 16 story points**

---

## Qué Debe Construirse y Para Qué

### US-20 · Matriz de Correlación

**¿Para qué?** Es el insumo principal del **heatmap de correlación** del dashboard. Permite visualizar qué activos del portafolio se mueven en bloque (alta correlación positiva), cuáles se mueven en sentido contrario (correlación negativa) y cuáles son independientes. Es fundamental para el análisis de diversificación de cartera.

**¿Qué hace este componente?**
- Orquesta llamadas al Algorithm Service para obtener el valor de Correlación de Pearson entre **todos los pares posibles** de los 20+ activos del portafolio.
- Construye y almacena una **matriz simétrica N×N** donde la celda `[i][j]` contiene el coeficiente de correlación entre el activo `i` y el activo `j`.
- La diagonal siempre es `1.0` (un activo correlaciona perfectamente consigo mismo).
- Expone esta matriz en un formato que el frontend pueda renderizar directamente como heatmap (ej. lista de objetos con `asset_a`, `asset_b`, `correlation`).

**Persistencia:** La matriz calculada debe guardarse en `report_db`. Debe existir un mecanismo para regenerarla cuando se actualicen los datos del ETL.

---

### US-23 · Generación de Reporte PDF

**¿Para qué?** El reporte PDF es un **entregable obligatorio del proyecto académico**. Es el documento exportable y auditable que consolida todos los hallazgos del sistema en un formato profesional. Un evaluador externo debe poder leer este PDF y comprender los resultados sin necesidad de acceder al dashboard.

**¿Qué contiene el reporte?**
El PDF debe consolidar, como mínimo:

1. **Portada** — Nombre del sistema, portafolio analizado, fecha de generación, horizonte histórico.
2. **Resumen del Portafolio** — Lista de los 20+ activos con su nombre, símbolo y mercado de origen (BVC / Global).
3. **Clasificación de Riesgo** — Tabla completa con el ranking de activos por volatilidad y su categoría (Conservador / Moderado / Agresivo).
4. **Análisis de Similitud** — Tabla resumen con los valores de los 4 algoritmos (Euclidiana, Pearson, DTW, Coseno) para los pares de activos más relevantes.
5. **Patrones Detectados** — Frecuencia de aparición de los dos patrones definidos (días consecutivos al alza y el patrón secundario formalizado) por activo.
6. **Análisis de Complejidad Formal (Big O)** — Sección técnica que documenta la complejidad temporal y espacial de cada algoritmo implementado en el sistema.
7. **Declaración de Uso de IA** — Sección de transparencia que detalla cómo se utilizaron herramientas de IA generativa en el desarrollo, confirmando que el diseño algorítmico y la arquitectura son de autoría humana.

**Nota técnica:** Para la generación del PDF en Java se puede usar una librería como **iText** o **Apache PDFBox**. La elección de librería para la generación del documento es libre; lo que no puede usarse es una librería que encapsule los *cálculos financieros*.

---

### US-24 · API REST del Report Service

**¿Para qué?** El gateway y el frontend no acceden directamente a la base de datos; todo pasa por los endpoints REST del Report Service. Esta historia define los contratos de la API que el frontend consumirá para construir el dashboard.

**Endpoints que deben existir:**

| Método | Path | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/reports/correlation-matrix` | Retorna la matriz de correlación completa del portafolio |
| `POST` | `/api/v1/reports/correlation-matrix/generate` | Fuerza el recálculo y persistencia de la matriz |
| `GET` | `/api/v1/reports/patterns/{symbol}` | Retorna la frecuencia de patrones detectados para un activo |
| `GET` | `/api/v1/reports/export/pdf` | Genera y descarga el reporte PDF completo |

Todos los endpoints deben retornar respuestas en **JSON** (excepto el PDF que retorna `application/pdf`). Deben incluir manejo de errores con códigos HTTP apropiados.

---

## Arquitectura Interna del Módulo

La estructura de paquetes a construir dentro de `report-service` debe seguir el mismo patrón de los otros microservicios del proyecto:

```
com.financial.report/
  ├── client/              ← Clientes HTTP para consumir ETL Service y Algorithm Service
  ├── config/              ← Configuración de RestTemplate, beans, etc.
  ├── controller/          ← Controladores REST (US-24)
  ├── dto/                 ← Objetos de transferencia de datos (request/response)
  ├── entity/              ← Entidades JPA para persistir en report_db
  ├── repository/          ← Repositorios Spring Data JPA
  ├── service/             ← Lógica de negocio de cada US
  ├── correlation/         ← Construcción de la matriz de correlación (US-20)
  └── pdf/                 ← Generación del reporte PDF (US-23)
```

---

## Base de Datos

El Report Service tiene su **propia base de datos** `report_db` (puerto `5432` según la configuración del docker-compose). Las tablas que debe gestionar este servicio son las necesarias para persistir:

- La matriz de correlación.
- Metadatos de reportes PDF generados (fecha de generación, activos incluidos, etc.).

El esquema debe definirse acorde al patrón ya establecido en los otros servicios del proyecto.

---

## Flujo de Dependencias

El Report Service **consume, no produce** datos de mercado. Su flujo es siempre:

```
ETL Service  ──────────────────────────────────────────────────────┐
                                                                    │
Algorithm Service  ── volatilidad, similitudes, patrones ──────────▼
                                                          Report Service
                                                               │
                                                     ┌─────────┴─────────┐
                                                     │                   │
                                                  Gateway              PDF
                                                     │
                                                  Frontend
                                              (Dashboard React)
```

El Report Service **nunca calcula algoritmos financieros directamente**. Si necesita un dato algorítmico, lo solicita al Algorithm Service. Si necesita datos OHLCV crudos, los solicita al ETL Service.

---

## Criterios de Aceptación (Definition of Done)

Una historia de esta epic está completa cuando:

- [ ] El código compila sin errores con `mvn clean install`.
- [ ] El microservicio arranca correctamente con Docker Compose.
- [ ] Los endpoints responden correctamente.
- [ ] Existen **tests unitarios** para la lógica de negocio (objetivo ≥80% de cobertura).
- [ ] Los datos se persisten correctamente en `report_db`.
- [ ] La comunicación REST con ETL Service y Algorithm Service está verificada.
- [ ] El reporte PDF incluye todas las secciones descritas en US-23.
- [ ] El análisis de complejidad Big O está documentado en el reporte PDF.

---

## Restricciones Técnicas que Aplican a esta Epic

1. **Prohibido usar librerías financieras de alto nivel** (yfinance, pandas_datareader, etc.) para ningún propósito dentro de este servicio.
2. **Los algoritmos de similitud y volatilidad NO se reimplementan aquí**: se consumen del Algorithm Service vía HTTP.
4. **No se aceptan datasets estáticos** (CSV hardcodeados): todos los datos vienen de llamadas a los otros microservicios.
5. **El reporte PDF es auditable**: cualquier evaluador debe poder ejecutar `GET /api/v1/reports/export/pdf` y obtener un documento con resultados reales y reproducibles.
