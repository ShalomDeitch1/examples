package com.example.shorturl.controller;

import com.example.shorturl.service.UrlService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/shorturl")
public class UrlController {

    private final UrlService urlService;

    @Value("${shorturl.base-url:}")
    private String baseUrl;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> shorten(@RequestBody Map<String, String> request) {
        String originalUrl = request.get("url");
        if (originalUrl == null || originalUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }

        String shortId = urlService.shorten(originalUrl);
        String shortUrl = buildShortUrl(shortId);
        return ResponseEntity.ok(Map.of("shortUrl", shortUrl));
    }

    @GetMapping("/{shortId}")
    public ResponseEntity<Void> resolve(@PathVariable String shortId) {
        return urlService.resolve(shortId)
                .map(url -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(url))
                        .<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{shortId}")
    public ResponseEntity<Void> delete(@PathVariable String shortId) {
        urlService.delete(shortId);
        return ResponseEntity.noContent().build();
    }

    private String buildShortUrl(String shortId) {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl + "/" + shortId;
        }
        // Fallback: build from current request
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/shorturl/" + shortId)
                .toUriString();
    }
}
