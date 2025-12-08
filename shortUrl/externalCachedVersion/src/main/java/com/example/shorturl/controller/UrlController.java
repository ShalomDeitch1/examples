package com.example.shorturl.controller;

import com.example.shorturl.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/shorturl")
public class UrlController {

    private final UrlService urlService;

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
        return ResponseEntity.ok(Map.of("shortUrl", shortId));
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
}
