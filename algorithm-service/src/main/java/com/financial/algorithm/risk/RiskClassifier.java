package com.financial.algorithm.risk;

import com.financial.algorithm.dto.RiskProfile;
import com.financial.algorithm.dto.RiskProfile.RiskCategory;
import com.financial.algorithm.dto.VolatilityResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Clasifica los activos de un portafolio por perfil de riesgo según su volatilidad anualizada.
 *
 * <p><b>Criterios de clasificación:</b>
 * <pre>
 *   CONSERVATIVE : σ_anual &lt; 0.15
 *   MODERATE     : 0.15 ≤ σ_anual ≤ 0.30
 *   AGGRESSIVE   : σ_anual &gt; 0.30
 * </pre>
 *
 * <p><b>Complejidad:</b> O(A · n) donde A es el número de activos y n el número de días.
 *
 * <p><b>Salida:</b> lista de {@link RiskProfile} ordenada ascendentemente por σ_anual.
 */
@Component
public class RiskClassifier {

    private static final double CONSERVATIVE_THRESHOLD = 0.15;
    private static final double AGGRESSIVE_THRESHOLD = 0.30;

    private final VolatilityCalculator volatilityCalculator;

    public RiskClassifier(VolatilityCalculator volatilityCalculator) {
        this.volatilityCalculator = volatilityCalculator;
    }

    /**
     * Clasifica todos los activos del portafolio y los ordena por volatilidad ascendente.
     *
     * @param portfolio mapa de ticker → precios de cierre en orden cronológico
     * @return lista de perfiles de riesgo ordenada de menor a mayor volatilidad
     */
    public List<RiskProfile> classify(Map<String, double[]> portfolio) {
        List<RiskProfile> profiles = new ArrayList<>();

        for (Map.Entry<String, double[]> entry : portfolio.entrySet()) {
            String ticker = entry.getKey();
            VolatilityResult vol = volatilityCalculator.compute(entry.getValue());
            double sigma = vol.getAnnualizedSigma();

            RiskCategory category;
            if (sigma < CONSERVATIVE_THRESHOLD) {
                category = RiskCategory.CONSERVATIVE;
            } else if (sigma <= AGGRESSIVE_THRESHOLD) {
                category = RiskCategory.MODERATE;
            } else {
                category = RiskCategory.AGGRESSIVE;
            }

            profiles.add(RiskProfile.builder()
                    .ticker(ticker)
                    .annualizedVolatility(sigma)
                    .category(category)
                    .build());
        }

        profiles.sort(Comparator.comparingDouble(RiskProfile::getAnnualizedVolatility));
        return profiles;
    }
}
