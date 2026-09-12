package cl.duoc.campuslab.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final RestClient restClient;

    public BookingController(RestClient.Builder builder,
                             @Value("${services.bookings.url}") String bookingsUrl) {
        this.restClient = builder.baseUrl(bookingsUrl).build();
    }

    @GetMapping
    public ResponseEntity<String> getAllBookings(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(required = false) String status) {

        String response = restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/bookings");
                    if (status != null && !status.isBlank()) {
                        builder.queryParam("status", status);
                    }
                    return builder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getBookingById(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        String response = restClient.get()
                .uri("/api/bookings/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<String> createBooking(
            @RequestBody String booking,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        String response = restClient.post()
                .uri("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(booking)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateBookingStatus(
            @PathVariable Long id,
            @RequestBody String statusBody,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        String response = restClient.put()
                .uri("/api/bookings/{id}/status", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(statusBody)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(response);
    }
}