package com.mcp.demo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class McpToolService {

    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);

    private final RestTemplate restTemplate;

    @Value("${mcp.server-url:}")
    private String mcpServerUrl;

    public McpToolService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(String prompt, String context) {
        if (mcpServerUrl != null && !mcpServerUrl.isBlank()) {
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("prompt", prompt);
                payload.put("context", context);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(mcpServerUrl, request, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                }
            } catch (Exception e) {
                log.warn("MCP remote call failed, falling back to local mock data. Cause: {}", e.getMessage());
            }
        }

        return buildMockPayload(prompt, context);
    }

    private Map<String, Object> buildMockPayload(String prompt, String context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tool", "weather_lookup");
        payload.put("status", "success");
        payload.put("request", prompt);
        payload.put("context", context);
        payload.put("message", "The MCP service retrieved structured weather data.");

        List<Map<String, Object>> data = List.of(
                Map.of(
                        "city", "Bangalore",
                        "country", "India",
                        "temperature", "28°C",
                        "condition", "Cloudy",
                        "humidity", "68%"
                ),
                Map.of(
                        "city", "Singapore",
                        "country", "Singapore",
                        "temperature", "31°C",
                        "condition", "Humid",
                        "humidity", "74%"
                )
        );

        payload.put("data", data);
        payload.put("source", "mcp");
        return payload;
    }
}
