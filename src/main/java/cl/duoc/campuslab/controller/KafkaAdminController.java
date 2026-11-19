
package cl.duoc.campuslab.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final RestClient restClient;

    public KafkaAdminController(
            RestClient.Builder builder,
            @Value("${services.kafka-admin.url}")
            String kafkaAdminUrl
    ) {
        this.restClient = builder
                .baseUrl(kafkaAdminUrl)
                .build();
    }

    // =====================================
    // 1. LISTAR TOPICOS
    // =====================================

    @GetMapping("/topics")
    public ResponseEntity<?> listTopics() {

        return forwardGet("/api/kafka/topics");
    }

    // =====================================
    // 2. CONSULTAR PARTICIONES Y REPLICAS
    // =====================================

    @GetMapping("/topics/{name}")
    public ResponseEntity<?> describeTopic(
            @PathVariable String name
    ) {

        return forwardGet(
                "/api/kafka/topics/{name}",
                name
        );
    }

    // =====================================
    // 3. LISTAR GRUPOS DE CONSUMIDORES
    // =====================================

    @GetMapping("/groups")
    public ResponseEntity<?> listGroups() {

        return forwardGet("/api/kafka/groups");
    }

    // =====================================
    // 4. REENVIAR PETICIONES GET
    // =====================================

    private ResponseEntity<?> forwardGet(String path) {

        try {

            String response = restClient
                    .get()
                    .uri(path)
                    .retrieve()
                    .body(String.class);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (RestClientResponseException ex) {

            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());
        }
    }

    // =====================================
    // 5. REENVIAR GET CON PARAMETRO
    // =====================================

    private ResponseEntity<?> forwardGet(
            String path,
            String topicName
    ) {

        try {

            String response = restClient
                    .get()
                    .uri(path, topicName)
                    .retrieve()
                    .body(String.class);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (RestClientResponseException ex) {

            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());
        }
    }
}
