package com.financial.algorithm.service;

import com.financial.algorithm.client.EtlServiceClient;
import com.financial.algorithm.dto.DashboardSummary;
import com.financial.algorithm.dto.PatternResult;
import com.financial.algorithm.dto.RiskProfile;
import com.financial.algorithm.entity.PatternRecord;
import com.financial.algorithm.entity.RiskRecord;
import com.financial.algorithm.entity.SimilarityRecord;
import com.financial.algorithm.entity.SmaRecord;
import com.financial.algorithm.indicators.SimpleMovingAverage;
import com.financial.algorithm.patterns.PatternConsecutiveUp;
import com.financial.algorithm.patterns.PatternMeanReversion;
import com.financial.algorithm.repository.PatternRepository;
import com.financial.algorithm.repository.RiskRepository;
import com.financial.algorithm.repository.SimilarityRepository;
import com.financial.algorithm.repository.SmaRepository;
import com.financial.algorithm.risk.RiskClassifier;
import com.financial.algorithm.similarity.CosineSimilarity;
import com.financial.algorithm.similarity.DynamicTimeWarping;
import com.financial.algorithm.similarity.EuclideanDistance;
import com.financial.algorithm.similarity.PearsonCorrelation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio principal que orquesta la ejecución completa del análisis algorítmico.
 *
 * <p>Flujo de {@link #runFullAnalysis()}:
 * <ol>
 *   <li>Obtiene precios limpios del ETL service via {@link EtlServiceClient}</li>
 *   <li>Calcula similitud entre todos los pares de activos y persiste los resultados</li>
 *   <li>Clasifica el riesgo de cada activo y persiste los perfiles</li>
 *   <li>Detecta patrones (consecutivos al alza y reversión a la media) y persiste</li>
 *   <li>Calcula la SMA(20) de cada activo y persiste los valores</li>
 * </ol>
 */
@Service
public class AlgorithmService {

    private static final List<Integer> SMA_WINDOWS = List.of(20, 50, 100);
    private static final int CONSECUTIVE_UP_K = 5;
    private static final double MEAN_REVERSION_WINDOW = 20;
    private static final double MEAN_REVERSION_THRESHOLD = 2.0;

    private final EtlServiceClient etlServiceClient;
    private final EuclideanDistance euclideanDistance;
    private final PearsonCorrelation pearsonCorrelation;
    private final DynamicTimeWarping dynamicTimeWarping;
    private final CosineSimilarity cosineSimilarity;
    private final RiskClassifier riskClassifier;
    private final PatternConsecutiveUp patternConsecutiveUp;
    private final PatternMeanReversion patternMeanReversion;
    private final SimpleMovingAverage simpleMovingAverage;
    private final SimilarityRepository similarityRepository;
    private final RiskRepository riskRepository;
    private final PatternRepository patternRepository;
    private final SmaRepository smaRepository;

    public AlgorithmService(EtlServiceClient etlServiceClient,
                            EuclideanDistance euclideanDistance,
                            PearsonCorrelation pearsonCorrelation,
                            DynamicTimeWarping dynamicTimeWarping,
                            CosineSimilarity cosineSimilarity,
                            RiskClassifier riskClassifier,
                            PatternConsecutiveUp patternConsecutiveUp,
                            PatternMeanReversion patternMeanReversion,
                            SimpleMovingAverage simpleMovingAverage,
                            SimilarityRepository similarityRepository,
                            RiskRepository riskRepository,
                            PatternRepository patternRepository,
                            SmaRepository smaRepository) {
        this.etlServiceClient = etlServiceClient;
        this.euclideanDistance = euclideanDistance;
        this.pearsonCorrelation = pearsonCorrelation;
        this.dynamicTimeWarping = dynamicTimeWarping;
        this.cosineSimilarity = cosineSimilarity;
        this.riskClassifier = riskClassifier;
        this.patternConsecutiveUp = patternConsecutiveUp;
        this.patternMeanReversion = patternMeanReversion;
        this.simpleMovingAverage = simpleMovingAverage;
        this.similarityRepository = similarityRepository;
        this.riskRepository = riskRepository;
        this.patternRepository = patternRepository;
        this.smaRepository = smaRepository;
    }

    /**
     * Ejecuta el análisis completo sobre todos los activos del portafolio.
     *
     * @return resumen con los conteos de registros persistidos por categoría
     */
    @Transactional
    public AnalysisSummary runFullAnalysis() {
        Map<String, double[]> portfolio = etlServiceClient.fetchCleanedPrices();

        List<String> tickers = new ArrayList<>(portfolio.keySet());
        LocalDateTime now = LocalDateTime.now();

        // Limpiar resultados anteriores antes de guardar los nuevos
        similarityRepository.deleteAll();
        riskRepository.deleteAll();
        patternRepository.deleteAll();
        smaRepository.deleteAll();

        // 1. Similitud: todos los pares únicos
        List<SimilarityRecord> similarities = computeSimilarities(portfolio, tickers, now);
        similarityRepository.saveAll(similarities);

        // 2. Riesgo
        List<RiskRecord> riskRecords = computeRisk(portfolio, now);
        riskRepository.saveAll(riskRecords);

        // 3. Patrones
        List<PatternRecord> patternRecords = computePatterns(portfolio, tickers, now);
        patternRepository.saveAll(patternRecords);

        // 4. SMA
        List<SmaRecord> smaRecords = computeSma(portfolio, tickers, now);
        smaRepository.saveAll(smaRecords);

        return new AnalysisSummary(similarities.size(), riskRecords.size(),
                patternRecords.size(), smaRecords.size());
    }

    // -------------------------------------------------------------------------
    // Métodos de acceso a últimos resultados persistidos
    // -------------------------------------------------------------------------

    /** Devuelve todos los resultados de similitud persistidos. */
    public List<SimilarityRecord> getSimilarityResults() {
        return similarityRepository.findAll();
    }

    /** Devuelve todos los perfiles de riesgo persistidos. */
    public List<RiskRecord> getRiskResults() {
        return riskRepository.findAll();
    }

    /** Devuelve los patrones de un símbolo concreto. */
    public List<PatternRecord> getPatternsBySymbol(String symbol) {
        return patternRepository.findBySymbol(symbol);
    }

    /** Devuelve todos los patrones persistidos sin filtro de símbolo. */
    public List<PatternRecord> getAllPatterns() {
        return patternRepository.findAll();
    }

    /** Devuelve el registro SMA más reciente para un símbolo y ventana. */
    public SmaRecord getSma(String symbol, int window) {
        return smaRepository.findTopBySymbolAndWindowOrderByComputedAtDesc(symbol, window)
                .orElse(null);
    }

    /** Devuelve un resumen consolidado para el dashboard del frontend. */
    public DashboardSummary getDashboardSummary() {
        long totalSimilarityPairs = similarityRepository.count();
        LocalDateTime lastAnalysisAt = riskRepository.findTopByOrderByComputedAtDesc()
                .map(RiskRecord::getComputedAt)
                .orElse(null);

        Map<String, Long> riskDistribution = new LinkedHashMap<>();
        for (RiskRepository.CategoryCount cc : riskRepository.countByCategory()) {
            riskDistribution.put(cc.getCategory(), cc.getTotal());
        }

        RiskRecord topRisk = riskRepository.findTopByOrderByAnnualizedVolatilityDesc().orElse(null);
        RiskRecord lowestRisk = riskRepository.findTopByOrderByAnnualizedVolatilityAsc().orElse(null);
        int totalAssets = (int) riskRepository.count();

        return new DashboardSummary(totalAssets, totalSimilarityPairs, lastAnalysisAt,
                riskDistribution, topRisk, lowestRisk);
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private List<SimilarityRecord> computeSimilarities(Map<String, double[]> portfolio,
                                                        List<String> tickers,
                                                        LocalDateTime now) {
        List<SimilarityRecord> records = new ArrayList<>();
        for (int i = 0; i < tickers.size(); i++) {
            for (int j = i + 1; j < tickers.size(); j++) {
                String ta = tickers.get(i);
                String tb = tickers.get(j);
                double[] ra = toReturns(portfolio.get(ta));
                double[] rb = toReturns(portfolio.get(tb));

                // Euclidean, Pearson y Cosine requieren arrays del mismo tamaño.
                // Distintos activos pueden tener diferente número de días de historia
                // (feriados locales, suspensiones de trading, fecha de inicio distinta).
                // Se trunca al mínimo común; DTW usa los arrays originales porque
                // maneja longitudes distintas de forma nativa.
                int minLen = Math.min(ra.length, rb.length);
                double[] raAligned = Arrays.copyOf(ra, minLen);
                double[] rbAligned = Arrays.copyOf(rb, minLen);

                SimilarityRecord rec = new SimilarityRecord();
                rec.setTickerA(ta);
                rec.setTickerB(tb);
                rec.setEuclidean(euclideanDistance.compute(raAligned, rbAligned));
                rec.setPearson(pearsonCorrelation.compute(raAligned, rbAligned));
                rec.setDtw(dynamicTimeWarping.compute(ra, rb));
                rec.setCosine(cosineSimilarity.compute(raAligned, rbAligned));
                rec.setComputedAt(now);
                records.add(rec);
            }
        }
        return records;
    }

    private List<RiskRecord> computeRisk(Map<String, double[]> portfolio, LocalDateTime now) {
        List<RiskProfile> profiles = riskClassifier.classify(portfolio);
        List<RiskRecord> records = new ArrayList<>();
        for (RiskProfile p : profiles) {
            RiskRecord rec = new RiskRecord();
            rec.setTicker(p.getTicker());
            rec.setAnnualizedVolatility(p.getAnnualizedVolatility());
            rec.setCategory(p.getCategory().name());
            rec.setComputedAt(now);
            records.add(rec);
        }
        return records;
    }

    private List<PatternRecord> computePatterns(Map<String, double[]> portfolio,
                                                 List<String> tickers,
                                                 LocalDateTime now) {
        List<PatternRecord> records = new ArrayList<>();
        for (String ticker : tickers) {
            double[] prices = portfolio.get(ticker);

            PatternResult up = patternConsecutiveUp.detect(prices, CONSECUTIVE_UP_K);
            records.add(toPatternRecord(ticker, up, now));

            PatternResult rev = patternMeanReversion.detect(prices,
                    (int) MEAN_REVERSION_WINDOW, MEAN_REVERSION_THRESHOLD);
            records.add(toPatternRecord(ticker, rev, now));
        }
        return records;
    }

    private PatternRecord toPatternRecord(String symbol, PatternResult result, LocalDateTime now) {
        PatternRecord rec = new PatternRecord();
        rec.setSymbol(symbol);
        rec.setPatternType(result.getPatternType());
        rec.setOccurrences(result.getOccurrences());
        rec.setRelativeFrequency(result.getRelativeFrequency());
        rec.setIndicesJson(buildIndicesJson(result));
        rec.setComputedAt(now);
        return rec;
    }

    private String buildIndicesJson(PatternResult result) {
        if (result.getDetails() == null || result.getDetails().isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < result.getDetails().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(result.getDetails().get(i).getStartIndex());
        }
        sb.append("]");
        return sb.toString();
    }

    private List<SmaRecord> computeSma(Map<String, double[]> portfolio,
                                        List<String> tickers,
                                        LocalDateTime now) {
        List<SmaRecord> records = new ArrayList<>();
        for (String ticker : tickers) {
            double[] prices = portfolio.get(ticker);
            for (int window : SMA_WINDOWS) {
                double[] values = simpleMovingAverage.compute(prices, window);
                SmaRecord rec = new SmaRecord();
                rec.setSymbol(ticker);
                rec.setWindow(window);
                rec.setValuesJson(Arrays.toString(values));
                rec.setComputedAt(now);
                records.add(rec);
            }
        }
        return records;
    }

    /** Convierte precios de cierre en retornos diarios. */
    private double[] toReturns(double[] prices) {
        if (prices.length < 2) return new double[0];
        double[] returns = new double[prices.length - 1];
        for (int i = 0; i < returns.length; i++) {
            returns[i] = (prices[i + 1] - prices[i]) / prices[i];
        }
        return returns;
    }

    /**
     * Resumen del análisis ejecutado.
     *
     * @param similarityPairs  número de pares de similitud persistidos
     * @param riskProfiles     número de perfiles de riesgo persistidos
     * @param patternRecords   número de registros de patrón persistidos
     * @param smaRecords       número de registros SMA persistidos
     */
    public record AnalysisSummary(int similarityPairs, int riskProfiles,
                                   int patternRecords, int smaRecords) {}
}
