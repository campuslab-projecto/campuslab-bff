
package cl.duoc.campuslab.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestController
@RequestMapping("/api/rabbit")
public class RabbitAdminController {

    private final RestClient restClient;

    public RabbitAdminController(
            RestClient.Builder builder,
            @Value("${services.rabbit-admin.url}")
            String rabbitAdminUrl
    ) {
        this.restClient = builder
                .baseUrl(rabbitAdminUrl)
                .build();
    }

    @PostMapping("/queues")
    public ResponseEntity<?> createQueue(
            @RequestBody Map<String, Object> request
    ) {
        return forwardPost("/api/rabbit/queues", request);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<?> deleteQueue(
            @PathVariable String name
    ) {
        return forwardDelete("/api/rabbit/queues/" + name);
    }

    @PostMapping("/exchanges")
    public ResponseEntity<?> createExchange(
            @RequestBody Map<String, Object> request
    ) {
        return forwardPost("/api/rabbit/exchanges", request);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<?> deleteExchange(
            @PathVariable String name
    ) {
        return forwardDelete("/api/rabbit/exchanges/" + name);
    }

    @PostMapping("/bindings")
    public ResponseEntity<?> createBinding(
            @RequestBody Map<String, Object> request
    ) {
        return forwardPost("/api/rabbit/bindings", request);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<?> deleteBinding(
            @RequestBody Map<String, Object> request
    ) {
        try {
            String result = restClient
                    .method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/api/rabbit/bindings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            return ResponseEntity.ok(result);

        } catch (RestClientResponseException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());
        }
    }

    private ResponseEntity<?> forwardPost(
            String path,
            Map<String, Object> request
    ) {
        try {
            String result = restClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            return ResponseEntity.ok(result);

        } catch (RestClientResponseException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());
        }
    }

    private ResponseEntity<?> forwardDelete(String path) {
        try {
            String result = restClient
                    .delete()
                    .uri(path)
                    .retrieve()
                    .body(String.class);

            return ResponseEntity.ok(result);

        } catch (RestClientResponseException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());
        }
    }
}
