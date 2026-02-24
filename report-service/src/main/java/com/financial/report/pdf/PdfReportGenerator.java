package com.financial.report.pdf;

import com.financial.report.dto.PatternResponse;
import com.financial.report.dto.RiskResponse;
import com.financial.report.dto.SimilarityResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generador de reportes PDF con todos los hallazgos del sistema financiero.
 *
 * <p>Produce un documento PDF auditatable con las siguientes secciones:
 * <ol>
 *   <li>Portada</li>
 *   <li>Resumen del Portafolio</li>
 *   <li>Clasificación de Riesgo</li>
 *   <li>Análisis de Similitud (top 10 por Pearson)</li>
 *   <li>Patrones Detectados</li>
 *   <li>Complejidad Algorítmica Big O</li>
 *   <li>Declaración de Uso de IA</li>
 * </ol>
 *
 * <p>Lógica pura sin HTTP ni BD — completamente testeable de forma aislada.
 */
@Component
public class PdfReportGenerator {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 20, Font.BOLD, Color.DARK_GRAY);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 13, Font.BOLD, Color.DARK_GRAY);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Color HEADER_BG = new Color(44, 62, 80);
    private static final Color ROW_ALT_BG = new Color(236, 240, 241);

    /**
     * Genera el reporte PDF completo a partir de los datos ya persistidos.
     *
     * @param risks        perfiles de riesgo de todos los activos
     * @param similarities resultados de similitud entre pares de activos
     * @param patterns     patrones detectados en todos los activos
     * @return array de bytes con el contenido del PDF
     */
    public byte[] generate(List<RiskResponse> risks,
                           List<SimilarityResponse> similarities,
                           List<PatternResponse> patterns) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(document, out);
        document.open();

        addCoverPage(document, risks);
        document.newPage();
        addPortfolioSummary(document, risks);
        addRiskClassification(document, risks);
        addSimilarityAnalysis(document, similarities);
        addPatterns(document, patterns);
        addBigOTable(document);
        addAiDeclaration(document);

        document.close();
        return out.toByteArray();
    }

    // -------------------------------------------------------------------------
    // Sección 1: Portada
    // -------------------------------------------------------------------------

    private void addCoverPage(Document document, List<RiskResponse> risks) throws DocumentException {
        document.add(new Paragraph("\n\n\n"));
        Paragraph title = new Paragraph("Sistema de Análisis Financiero", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Reporte de Hallazgos del Portafolio", SUBTITLE_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingBefore(10);
        document.add(subtitle);

        document.add(new Paragraph("\n\n"));

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Paragraph date = new Paragraph("Fecha de generación: " + dateStr, BODY_FONT);
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);

        int assetCount = risks != null ? (int) risks.stream().map(RiskResponse::getTicker).distinct().count() : 0;
        Paragraph assets = new Paragraph("Portafolio: " + assetCount + " activos analizados", BODY_FONT);
        assets.setAlignment(Element.ALIGN_CENTER);
        assets.setSpacingBefore(6);
        document.add(assets);
    }

    // -------------------------------------------------------------------------
    // Sección 2: Resumen del Portafolio
    // -------------------------------------------------------------------------

    private void addPortfolioSummary(Document document, List<RiskResponse> risks) throws DocumentException {
        addSectionHeader(document, "Resumen del Portafolio");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(70);
        table.setSpacingBefore(8);
        addTableHeader(table, "Ticker", "Categoría de Riesgo");

        if (risks != null) {
            boolean alt = false;
            for (RiskResponse r : risks) {
                Color bg = alt ? ROW_ALT_BG : Color.WHITE;
                addCell(table, r.getTicker(), bg);
                addCell(table, r.getCategory(), bg);
                alt = !alt;
            }
        }
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    // -------------------------------------------------------------------------
    // Sección 3: Clasificación de Riesgo
    // -------------------------------------------------------------------------

    private void addRiskClassification(Document document, List<RiskResponse> risks) throws DocumentException {
        addSectionHeader(document, "Clasificación de Riesgo");

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(90);
        table.setSpacingBefore(8);
        addTableHeader(table, "Ticker", "Volatilidad Anualizada", "Categoría");

        if (risks != null) {
            boolean alt = false;
            for (RiskResponse r : risks) {
                Color bg = alt ? ROW_ALT_BG : Color.WHITE;
                addCell(table, r.getTicker(), bg);
                addCell(table, String.format("%.4f", r.getAnnualizedVolatility()), bg);
                addCell(table, r.getCategory(), bg);
                alt = !alt;
            }
        }
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    // -------------------------------------------------------------------------
    // Sección 4: Análisis de Similitud (top-10 por Pearson desc)
    // -------------------------------------------------------------------------

    private void addSimilarityAnalysis(Document document, List<SimilarityResponse> similarities) throws DocumentException {
        addSectionHeader(document, "Análisis de Similitud");

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        addTableHeader(table, "Activo A", "Activo B", "Euclidiana", "Pearson", "DTW", "Coseno");

        if (similarities != null) {
            List<SimilarityResponse> top10 = similarities.stream()
                    .sorted(Comparator.comparingDouble(SimilarityResponse::getPearson).reversed())
                    .limit(10)
                    .collect(Collectors.toList());

            boolean alt = false;
            for (SimilarityResponse s : top10) {
                Color bg = alt ? ROW_ALT_BG : Color.WHITE;
                addCell(table, s.getTickerA(), bg);
                addCell(table, s.getTickerB(), bg);
                addCell(table, String.format("%.4f", s.getEuclidean()), bg);
                addCell(table, String.format("%.4f", s.getPearson()), bg);
                addCell(table, String.format("%.4f", s.getDtw()), bg);
                addCell(table, String.format("%.4f", s.getCosine()), bg);
                alt = !alt;
            }
        }
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    // -------------------------------------------------------------------------
    // Sección 5: Patrones Detectados
    // -------------------------------------------------------------------------

    private void addPatterns(Document document, List<PatternResponse> patterns) throws DocumentException {
        addSectionHeader(document, "Patrones Detectados");

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        addTableHeader(table, "Activo", "Tipo Patrón", "Ocurrencias", "Frecuencia Relativa");

        if (patterns != null) {
            boolean alt = false;
            for (PatternResponse p : patterns) {
                Color bg = alt ? ROW_ALT_BG : Color.WHITE;
                addCell(table, p.getSymbol(), bg);
                addCell(table, p.getPatternType(), bg);
                addCell(table, String.valueOf(p.getOccurrences()), bg);
                addCell(table, String.format("%.4f", p.getRelativeFrequency()), bg);
                alt = !alt;
            }
        }
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    // -------------------------------------------------------------------------
    // Sección 6: Complejidad Algorítmica Big O
    // -------------------------------------------------------------------------

    private void addBigOTable(Document document) throws DocumentException {
        addSectionHeader(document, "Complejidad Algorítmica Big O");

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        addTableHeader(table, "Algoritmo", "Complejidad Temporal", "Complejidad Espacial");

        Object[][] rows = {
            {"Pearson / Coseno / Euclidiana", "O(n)", "O(1)"},
            {"Dynamic Time Warping (DTW)", "O(n²)", "O(n²)"},
            {"Simple Moving Average", "O(n)", "O(k)"},
            {"Z-score Anomaly Detector", "O(n)", "O(1)"},
            {"Pattern Consecutive Up", "O(n·k)", "O(1)"},
            {"Pattern Mean Reversion", "O(n·w)", "O(w)"}
        };

        boolean alt = false;
        for (Object[] row : rows) {
            Color bg = alt ? ROW_ALT_BG : Color.WHITE;
            addCell(table, (String) row[0], bg);
            addCell(table, (String) row[1], bg);
            addCell(table, (String) row[2], bg);
            alt = !alt;
        }
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    // -------------------------------------------------------------------------
    // Sección 7: Declaración de Uso de IA
    // -------------------------------------------------------------------------

    private void addAiDeclaration(Document document) throws DocumentException {
        addSectionHeader(document, "Declaración de Uso de Inteligencia Artificial");

        String declaration =
            "Este proyecto utilizó herramientas de Inteligencia Artificial (IA) como asistente de " +
            "codificación durante su desarrollo. El diseño algorítmico, la arquitectura del sistema, " +
            "la lógica de negocio y las decisiones técnicas fundamentales son de autoría humana. " +
            "La IA fue empleada exclusivamente como apoyo en la generación de código repetitivo, " +
            "revisión de sintaxis y sugerencias de implementación, sin reemplazar el juicio " +
            "profesional ni la comprensión conceptual de los algoritmos implementados.";

        Paragraph p = new Paragraph(declaration, BODY_FONT);
        p.setSpacingBefore(8);
        p.setLeading(14);
        document.add(p);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void addSectionHeader(Document document, String title) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingBefore(12);
        PdfPCell cell = new PdfPCell(new Phrase(title, SECTION_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(cell);
        document.add(headerTable);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addCell(PdfPTable table, String value, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", BODY_FONT));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        table.addCell(cell);
    }
}
