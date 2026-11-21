
package cl.duoc.campuslab.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private static final String ADMIN_HEADER = "X-Admin-Key";

    private final RestClient restClient;
    private final String adminKey;

    public KafkaAdminController(
            RestClient.Builder builder,
            @Value("${services.kafka-admin.url}") String kafkaAdminUrl,
            @Value("${KAFKA_ADMIN_API_KEY:}") String adminKey
    ) {
        this.restClient = builder
                .baseUrl(kafkaAdminUrl)
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();

        this.adminKey = adminKey;
    }

    // ==========================================
    // 1. LISTAR TOPICOS
    // ==========================================

    @GetMapping("/topics")
    public ResponseEntity<?> listTopics() {
        return forwardGet("/api/kafka/topics");
    }

    // ==========================================
    // 2. CONSULTAR PARTICIONES Y REPLICAS
    // ==========================================

    @GetMapping("/topics/{name}")
    public ResponseEntity<?> describeTopic(
            @PathVariable String name
    ) {
        return forwardGet(
                "/api/kafka/topics/{name}",
                name
        );
    }

    // ==========================================
    // 3. LISTAR GRUPOS DE CONSUMIDORES
    // ==========================================

    @GetMapping("/groups")
    public ResponseEntity<?> listGroups() {
        return forwardGet("/api/kafka/groups");
    }

    // ==========================================
    // 4. MONITOREAR OFFSETS DLT
    // ==========================================

    @GetMapping("/dlt/offsets")
    public ResponseEntity<?> getDltOffsets() {
        return forwardGet("/api/kafka/dlt/offsets");
    }

    // ==========================================
    // 5. CREAR TOPICO DE DEMOSTRACION
    // ==========================================

    @PostMapping("/topics")
    public ResponseEntity<?> createTopic(
            Authentication authentication,
            @RequestBody CreateTopicRequest request
    ) {
        validateAdmin(authentication);

        return forwardWrite(
                HttpMethod.POST,
                "/api/kafka/topics",
                null,
                request
        );
    }

    // ==========================================
    // 6. CONSULTAR CONFIGURACION DEL TOPICO
    // ==========================================

    @GetMapping("/topics/{name}/config")
    public ResponseEntity<?> getTopicConfig(
            Authentication authentication,
            @PathVariable String name
    ) {
        validateAdmin(authentication);

        try {
            return restClient
                    .get()
                    .uri("/api/kafka/topics/{name}/config", name)
                    .header(ADMIN_HEADER, adminKey)
                    .retrieve()
                    .toEntity(String.class);

        } catch (RestClientResponseException ex) {
            return downstreamError(ex);

        } catch (RestClientException ex) {
            return gatewayError();
        }
    }

    // ==========================================
    // 7. MODIFICAR CONFIGURACION
    // ==========================================

    @PutMapping("/topics/{name}/config")
    public ResponseEntity<?> updateTopicConfig(
            Authentication authentication,
            @PathVariable String name,
            @RequestBody UpdateTopicConfigRequest request
    ) {
        validateAdmin(authentication);

        return forwardWrite(
                HttpMethod.PUT,
                "/api/kafka/topics/{name}/config",
                name,
                request
        );
    }

    // ==========================================
    // 8. AUMENTAR PARTICIONES
    // ==========================================

    @PatchMapping("/topics/{name}/partitions")
    public ResponseEntity<?> updatePartitions(
            Authentication authentication,
            @PathVariable String name,
            @RequestBody UpdatePartitionsRequest request
    ) {
        validateAdmin(authentication);

        return forwardWrite(
                HttpMethod.PATCH,
                "/api/kafka/topics/{name}/partitions",
                name,
                request
        );
    }

    // ==========================================
    // 9. ELIMINAR TOPICO DE DEMOSTRACION
    // ==========================================

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<?> deleteTopic(
            Authentication authentication,
            @PathVariable String name,
            @RequestParam("confirm") String confirmation
    ) {
        validateAdmin(authentication);

        try {
            return restClient
                    .delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/kafka/topics/{name}")
                            .queryParam("confirm", confirmation)
                            .build(name))
                    .header(ADMIN_HEADER, adminKey)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException ex) {
            return downstreamError(ex);

        } catch (RestClientException ex) {
            return gatewayError();
        }
    }

    // ==========================================
    // 10. VALIDAR ADMINISTRADOR
    // ==========================================

    private void validateAdmin(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                .noneMatch(authority ->
                        "ROLE_ADMIN".equals(
                                authority.getAuthority()
                        ))) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo administradores pueden gestionar topicos"
            );
        }

        if (adminKey == null || adminKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Clave administrativa no configurada en BFF"
            );
        }
    }

    // ==========================================
    // 11. REENVIAR CONSULTAS GET
    // ==========================================

    private ResponseEntity<?> forwardGet(String path) {
        try {
            return restClient
                    .get()
                    .uri(path)
                    .retrieve()
                    .toEntity(String.class);

        } catch (RestClientResponseException ex) {
            return downstreamError(ex);

        } catch (RestClientException ex) {
            return gatewayError();
        }
    }

    private ResponseEntity<?> forwardGet(
            String path,
            String topicName
    ) {
        try {
            return restClient
                    .get()
                    .uri(path, topicName)
                    .retrieve()
                    .toEntity(String.class);

        } catch (RestClientResponseException ex) {
            return downstreamError(ex);

        } catch (RestClientException ex) {
            return gatewayError();
        }
    }

    // ==========================================
    // 12. REENVIAR POST, PUT Y PATCH
    // ==========================================

    private ResponseEntity<?> forwardWrite(
            HttpMethod method,
            String path,
            String topicName,
            Object request
    ) {
        try {
            RestClient.RequestBodySpec specification;

            if (topicName == null) {
                specification = restClient
                        .method(method)
                        .uri(path);
            } else {
                specification = restClient
                        .method(method)
                        .uri(path, topicName);
            }

            return specification
                    .header(ADMIN_HEADER, adminKey)
                    .contentType(
                            org.springframework.http.MediaType
                                    .APPLICATION_JSON
                    )
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

        } catch (RestClientResponseException ex) {
            return downstreamError(ex);

        } catch (RestClientException ex) {
            System.err.println(
                    "ERROR KAFKA ADMIN [" + method + "]: "
                            + ex.getClass().getName()
                            + " - " + ex.getMessage()
            );

            return gatewayError();
        }
    }

    // ==========================================
    // 13. MANEJO DE ERRORES DEL MICROSERVICIO
    // ==========================================

    private ResponseEntity<?> downstreamError(
            RestClientResponseException ex
    ) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ex.getResponseBodyAsString());
    }

    private ResponseEntity<?> gatewayError() {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error",
                        "No fue posible comunicarse con MS Kafka Admin"
                ));
    }

    // ==========================================
    // 14. DTO PARA CREAR TOPICO
    // ==========================================

    public record CreateTopicRequest(
            String name,
            Integer partitions,
            Short replicationFactor
    ) {
    }

    // ==========================================
    // 15. DTO PARA MODIFICAR CONFIGURACION
    // ==========================================

    public record UpdateTopicConfigRequest(
            Long retentionMs,
            String cleanupPolicy
    ) {
    }

    // ==========================================
    // 16. DTO PARA AUMENTAR PARTICIONES
    // ==========================================

    public record UpdatePartitionsRequest(
            Integer partitions
    ) {
    }
}
