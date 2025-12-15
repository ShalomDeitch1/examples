package com.example.rollingChunks.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.rollingChunks.service.ChangeFeedService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/changes")
public class ChangeFeedController {

    private final ChangeFeedService changeFeedService;

    public ChangeFeedController(ChangeFeedService changeFeedService) {
        this.changeFeedService = changeFeedService;
    }

    @GetMapping
    public ResponseEntity<ChangeFeedService.ChangeFeedResponse> getChanges(
            @RequestParam("deviceId") String deviceId,
            @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(changeFeedService.getChanges(deviceId, limit));
    }

    @PostMapping("/ack")
    public ResponseEntity<ChangeFeedService.DeviceCheckpointResponse> ack(@Valid @RequestBody AckRequest req) {
        return ResponseEntity.ok(changeFeedService.ack(req.deviceId(), req.lastSeenEventId()));
    }

    public record AckRequest(
            @NotBlank String deviceId,
            @Min(0) long lastSeenEventId
    ) {}
}
