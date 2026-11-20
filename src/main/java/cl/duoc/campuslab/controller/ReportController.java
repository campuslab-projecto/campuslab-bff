
package cl.duoc.campuslab.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final RestClient restClient;

    public ReportController(
            RestClient.Builder builder,
            @Value("${services.report.url}") String reportUrl
    ) {
        this.restClient = builder
                .baseUrl(reportUrl)
                .build();
    }

    // ======================================
    // INDICADORES PROCESADOS POR KAFKA
    // ======================================

    @GetMapping("/kpis")
    public ResponseEntity<String> obtenerKpis() {

        String response = restClient.get()
                .uri("/api/report/kpis")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // ======================================
    // RECURSOS MAS UTILIZADOS
    // ======================================

    @GetMapping("/top-resources")
    public ResponseEntity<String> obtenerTopRecursos() {

        String response = restClient.get()
                .uri("/api/report/top-resources")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
