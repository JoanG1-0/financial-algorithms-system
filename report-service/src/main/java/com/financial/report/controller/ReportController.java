package com.financial.report.controller;

import com.financial.report.entity.CorrelationEntry;
import com.financial.report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST del report-service.
 *
 * <table border="1">
 *   <tr><th>Método</th><th>Endpoint</th><th>Descripción</th><th>Status</th></tr>
 *   <tr><td>POST</td><td>/api/v1/reports/correlation-matrix/generate</td><td>Genera la matriz de correlación</td><td>202</td></tr>
 *   <tr><td>GET</td><td>/api/v1/reports/correlation-matrix</td><td>Consulta la matriz persistida</td><td>200</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Dispara la generación (o regeneración) de la matriz de correlación de Pearson.
     *
     * @return 202 Accepted con el resumen de entradas persistidas
     */
    @PostMapping("/correlation-matrix/generate")
    public ResponseEntity<ReportService.CorrelationSummary> generate() {
        ReportService.CorrelationSummary summary = reportService.generateCorrelationMatrix();
        return ResponseEntity.accepted().body(summary);
    }

    /**
     * Devuelve todas las entradas de la matriz de correlación persistidas.
     *
     * @return 200 OK con lista de {@link CorrelationEntry}
     */
    @GetMapping("/correlation-matrix")
    public ResponseEntity<List<CorrelationEntry>> correlationMatrix() {
        return ResponseEntity.ok(reportService.getCorrelationMatrix());
    }
}
