
package cl.duoc.campuslab.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BookingController(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${services.bookings.url}") String bookingsUrl
    ) {
        this.restClient = builder.baseUrl(bookingsUrl).build();
        this.objectMapper = objectMapper;
    }

    // ==========================================
    // 1. IDENTIFICAR PERMISOS DEL USUARIO
    // ==========================================

    private boolean canManageBookings(
            JwtAuthenticationToken authentication
    ) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_TECNICO")
                                || authority.getAuthority().equals("ROLE_ADMIN")
                );
    }

    // ==========================================
    // 2. OBTENER CORREO DEL JWT
    // ==========================================

    private String getAuthenticatedEmail(
            JwtAuthenticationToken authentication
    ) {

        Jwt jwt = authentication.getToken();

        String[] emailClaims = {
                "preferred_username",
                "upn",
                "email"
        };

        for (String claimName : emailClaims) {

            String value = jwt.getClaimAsString(claimName);

            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El token no contiene un correo identificable."
        );
    }

    private boolean isOwner(JsonNode booking, String email) {

        String bookingEmail = booking
                .path("userEmail")
                .asText("");

        return bookingEmail.trim().equalsIgnoreCase(email);
    }

    // ==========================================
    // 3. UTILIDADES JSON
    // ==========================================

    private JsonNode readJson(String json) {

        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException | IllegalArgumentException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El microservicio devolvio una respuesta JSON invalida.",
                    e
            );
        }
    }

    private ObjectNode readBookingRequest(String json) {

        try {

            JsonNode node = objectMapper.readTree(json);

            if (!node.isObject()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La reserva debe ser un objeto JSON."
                );
            }

            return (ObjectNode) node;

        } catch (JsonProcessingException | IllegalArgumentException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El cuerpo de la reserva no es JSON valido.",
                    e
            );
        }
    }

    private String toJson(JsonNode node) {

        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible generar la respuesta JSON.",
                    e
            );
        }
    }

    private ResponseEntity<String> jsonResponse(String json) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    // ==========================================
    // 4. LISTAR RESERVAS
    // ==========================================

    @GetMapping
    public ResponseEntity<String> getAllBookings(

            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization,

            @RequestParam(required = false)
            String status,

            JwtAuthenticationToken authentication
    ) {

        String response = restClient.get()
                .uri(uriBuilder -> {

                    var uri = uriBuilder.path("/api/bookings");

                    if (status != null && !status.isBlank()) {
                        uri.queryParam("status", status);
                    }

                    return uri.build();
                })
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(String.class);

        // Tecnico y administrador pueden consultar el listado.
        if (canManageBookings(authentication)) {
            return jsonResponse(response);
        }

        // Estudiante: devolver solamente sus propias reservas.
        String userEmail = getAuthenticatedEmail(authentication);

        JsonNode result = readJson(response);

        if (!result.isArray()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Se esperaba una lista de reservas."
            );
        }

        ArrayNode ownBookings = objectMapper.createArrayNode();

        for (JsonNode booking : result) {

            if (isOwner(booking, userEmail)) {
                ownBookings.add(booking);
            }
        }

        return jsonResponse(toJson(ownBookings));
    }

    // ==========================================
    // 5. CONSULTAR RESERVA POR ID
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<String> getBookingById(

            @PathVariable Long id,

            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization,

            JwtAuthenticationToken authentication
    ) {

        String response = restClient.get()
                .uri("/api/bookings/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(String.class);

        if (canManageBookings(authentication)) {
            return jsonResponse(response);
        }

        JsonNode booking = readJson(response);

        String userEmail = getAuthenticatedEmail(authentication);

        if (!isOwner(booking, userEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reserva no encontrada."
            );
        }

        return jsonResponse(response);
    }

    // ==========================================
    // 6. CREAR RESERVA
    // ==========================================

    @PostMapping
    public ResponseEntity<String> createBooking(

            @RequestBody String bookingBody,

            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization,

            JwtAuthenticationToken authentication
    ) {

        ObjectNode booking = readBookingRequest(bookingBody);

        String resourceId = booking
                .path("resourceId")
                .asText("")
                .trim();

        if (resourceId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar el laboratorio o equipo."
            );
        }

        if (!canManageBookings(authentication)) {

            // El estudiante no puede suplantar otro correo.
            booking.put(
                    "userEmail",
                    getAuthenticatedEmail(authentication)
            );

        } else {

            String email = booking
                    .path("userEmail")
                    .asText("")
                    .trim();

            if (email.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Debes indicar el correo del estudiante."
                );
            }

            booking.put("userEmail", email);
        }

        booking.put("resourceId", resourceId);

        // Toda reserva nueva comienza como SOLICITADA.
        booking.put("status", "SOLICITADA");

        // El servidor debe asignar el ID.
        booking.remove("id");

        String response = restClient.post()
                .uri("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(booking))
                .retrieve()
                .body(String.class);

        return jsonResponse(response);
    }

    // ==========================================
    // 7. CAMBIAR ESTADO DE RESERVA
    // ==========================================

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateBookingStatus(

            @PathVariable Long id,

            @RequestBody String statusBody,

            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization,

            JwtAuthenticationToken authentication
    ) {

        // Segunda comprobacion de permisos.
        if (!canManageBookings(authentication)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permisos para cambiar estados."
            );
        }

        String response = restClient.put()
                .uri("/api/bookings/{id}/status", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(statusBody)
                .retrieve()
                .body(String.class);

        return jsonResponse(response);
    }
}
