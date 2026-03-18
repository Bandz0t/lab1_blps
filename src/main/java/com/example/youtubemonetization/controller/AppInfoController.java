package com.example.youtubemonetization.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppInfoController {

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("application", "youtube-monetization");
        response.put("status", "UP");
        response.put("message", "Service is running. Use REST API endpoints under /api.");
        response.put("docs", new String[] {
                "POST /api/videos",
                "GET /api/videos/{id}",
                "GET /api/videos/{id}/status",
                "POST /api/videos/{id}/copyright-check",
                "POST /api/videos/{id}/monetization",
                "GET /api/users/{userId}/revenues",
                "GET /api/users/{userId}/payouts",
                "POST /api/payouts/process-monthly",
                "GET /api/processes/{videoId}"
        });
        return response;
    }
}
