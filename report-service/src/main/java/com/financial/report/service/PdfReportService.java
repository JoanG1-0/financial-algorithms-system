package com.financial.report.service;

import com.financial.report.client.AlgorithmServiceClient;
import com.financial.report.dto.PatternResponse;
import com.financial.report.dto.RiskResponse;
import com.financial.report.dto.SimilarityResponse;
import com.financial.report.pdf.PdfReportGenerator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que orquesta la generación del reporte PDF completo.
 *
 * <p>Recopila los datos ya persistidos del algorithm-service y los
 * delega al {@link PdfReportGenerator} para construir el documento.
 */
@Service
public class PdfReportService {

    private final AlgorithmServiceClient client;
    private final PdfReportGenerator pdfReportGenerator;

    public PdfReportService(AlgorithmServiceClient client, PdfReportGenerator pdfReportGenerator) {
        this.client = client;
        this.pdfReportGenerator = pdfReportGenerator;
    }

    /**
     * Genera el reporte PDF con todos los hallazgos del sistema.
     *
     * @return array de bytes con el contenido del PDF
     */
    public byte[] generatePdf() {
        List<RiskResponse> risks = client.fetchRiskProfiles();
        List<SimilarityResponse> sims = client.fetchSimilarities();
        List<PatternResponse> patterns = client.fetchAllPatterns();
        return pdfReportGenerator.generate(risks, sims, patterns);
    }
}
