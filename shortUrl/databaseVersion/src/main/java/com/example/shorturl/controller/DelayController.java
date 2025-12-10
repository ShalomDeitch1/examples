package com.example.shorturl.controller;

import com.example.shorturl.aop.DelayManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/shorturl/config")
public class DelayController {

    private final DelayManager delayManager;

    public DelayController(DelayManager delayManager) {
        this.delayManager = delayManager;
    }

    @PostMapping("/delay")
    public ResponseEntity<Map<String, Integer>> configureDelay(
            @RequestParam(defaultValue = "0") int min,
            @RequestParam(defaultValue = "0") int max) {

        delayManager.setDelayRange(min, max);

        return ResponseEntity.ok(Map.of(
                "minDelayMs", min,
                "maxDelayMs", max));
    }
}
