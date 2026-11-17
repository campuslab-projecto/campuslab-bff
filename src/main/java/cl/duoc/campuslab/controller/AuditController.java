
package cl.duoc.campuslab.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final RestClient restClient;

    public AuditController(
            RestClient.Builder builder,
            @Value("${services.audit.url}") String auditUrl
    ) {
        this.restClient = builder
                .baseUrl(auditUrl)
                .build();
    }

    // ==========================================
    // 1. OBTENER TODOS LOS EVENTOS
    // ==========================================

    @GetMapping
    public ResponseEntity<String> getAllEvents() {

        String response = restClient.get()
                .uri("/api/audit")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // ==========================================
    // 2. OBTENER EVENTOS DE UNA RESERVA
    // ==========================================

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<String> getEventsByBookingId(
            @PathVariable Long bookingId
    ) {

        String response = restClient.get()
                .uri("/api/audit/bookings/{bookingId}", bookingId)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
