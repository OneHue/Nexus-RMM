package com.nexus.server.health.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Rest controller comment tells Spring that this class handles web
// requests and returns response data

@RestController
public class HealthController {

    // Associates an HTTP GET request to /api/v1/health with the health() method
    @GetMapping("/api/v1/health")
    public Map<String, String> health() {

        // Returns a map containing status of the server
        return Map.of(
                "status", "UP",
                "application", "Nexus"
        );
    }
}
