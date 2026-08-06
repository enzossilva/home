package com.example.demo.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Same-origin image proxy so the frontend can strip studio backgrounds via canvas
 * without hitting Cloudinary CORS limits.
 */
@RestController
public class ImageProxyController {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "res.cloudinary.com",
            "images.unsplash.com",
            "i.imgur.com"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @GetMapping("/img-proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam("url") String url) {
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

            if (!scheme.equals("https") && !scheme.equals("http")) {
                return ResponseEntity.badRequest().build();
            }
            if (!isAllowedHost(host)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "YoungZoneImageProxy/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                return ResponseEntity.status(response.statusCode()).build();
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(response.body());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    private boolean isAllowedHost(String host) {
        if (ALLOWED_HOSTS.contains(host)) return true;
        // Allow Cloudinary account subdomains if any
        return host.endsWith(".cloudinary.com");
    }
}
