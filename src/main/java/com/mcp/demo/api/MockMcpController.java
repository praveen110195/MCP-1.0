package com.mcp.demo.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
public class MockMcpController {

    @GetMapping({"", "/", "/health"})
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "mcp-local-test");
    }

    @PostMapping({"", "/"})
    public Map<String, Object> execute(@RequestBody Map<String, Object> payload) {
        String prompt = payload == null || payload.get("prompt") == null ? "" : String.valueOf(payload.get("prompt"));
        String context = payload == null || payload.get("context") == null ? "Default workspace context" : String.valueOf(payload.get("context"));
        String lowerPrompt = prompt.toLowerCase(Locale.ROOT);
        String intent = detectIntent(lowerPrompt);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tool", "mcp-local-test");
        response.put("status", "success");
        response.put("request", prompt);
        response.put("context", context);
        response.put("source", "mcp-local-test");
        response.put("intent", intent);
        response.put("entities", extractEntities(prompt));
        response.put("summary", buildSummary(intent));
        response.put("recommendations", buildRecommendations(intent));
        response.put("data", buildData(intent, prompt));
        response.put("metadata", Map.of(
                "responseMode", "remote-mcp-test",
                "generatedAt", Instant.now().toString()
        ));
        return response;
    }

    private String detectIntent(String prompt) {
        if (prompt.contains("travel") || prompt.contains("flight") || prompt.contains("hotel") || prompt.contains("weather") || prompt.contains("trip")) {
            return "travel";
        }
        if (prompt.contains("stock") || prompt.contains("market") || prompt.contains("share") || prompt.contains("portfolio") || prompt.contains("invest")) {
            return "stock";
        }
        if (prompt.contains("sales") || prompt.contains("revenue") || prompt.contains("pipeline") || prompt.contains("quota") || prompt.contains("customer")) {
            return "sales";
        }
        if (prompt.contains("support") || prompt.contains("ticket") || prompt.contains("issue") || prompt.contains("incident") || prompt.contains("sla")) {
            return "support";
        }
        return "general";
    }

    private List<String> extractEntities(String prompt) {
        List<String> entities = new ArrayList<>();
        if (prompt == null || prompt.isBlank()) {
            return entities;
        }

        String[] tokens = prompt.split("[^A-Za-z0-9.-]+");
        for (String token : tokens) {
            if (!token.isBlank() && token.length() > 2) {
                entities.add(token);
            }
        }

        if (entities.isEmpty()) {
            entities.add("General Business Context");
        }
        return entities.stream().distinct().limit(8).toList();
    }

    private String buildSummary(String intent) {
        if ("travel".equals(intent)) {
            return "Travel intent detected: destination and itinerary data were validated by the local MCP test endpoint.";
        }
        if ("stock".equals(intent)) {
            return "Market intent detected: portfolio and price movement data were validated by the local MCP test endpoint.";
        }
        if ("sales".equals(intent)) {
            return "Sales intent detected: pipeline and revenue data were validated by the local MCP test endpoint.";
        }
        if ("support".equals(intent)) {
            return "Support intent detected: ticket and SLA values were validated by the local MCP test endpoint.";
        }
        return "General business intent detected: the local MCP test endpoint returned structured operational data.";
    }

    private List<String> buildRecommendations(String intent) {
        if ("travel".equals(intent)) {
            return List.of("Confirm weather and flight status before departure.", "Recheck hotel readiness and transfer timing.", "Share the itinerary with all stakeholders.");
        }
        if ("stock".equals(intent)) {
            return List.of("Review price momentum versus the weekly trend.", "Check volatility and sentiment before the next trade.", "Set alerts for breakout and breakdown levels.");
        }
        if ("sales".equals(intent)) {
            return List.of("Prioritize the highest-probability deals.", "Review discount variance against target margin.", "Schedule follow-up meetings with delayed accounts.");
        }
        if ("support".equals(intent)) {
            return List.of("Group tickets by urgency and SLA risk.", "Escalate the highest-risk incidents to senior responders.", "Share a customer update after the root cause review.");
        }
        return List.of("Validate source data before publishing insights.", "Use the response as a working summary.", "Review operational context before action.");
    }

    private List<Map<String, Object>> buildData(String intent, String prompt) {
        if ("travel".equals(intent)) {
            return List.of(
                    Map.of(
                            "city", "Bangalore",
                            "country", "India",
                            "temperature", "28°C",
                            "condition", "Cloudy",
                            "flightStatus", "On time",
                            "hotelReadiness", "Ready",
                            "recommendation", "Good for a relaxed itinerary"
                    )
            );
        }
        if ("stock".equals(intent)) {
            return List.of(
                    Map.of(
                            "symbol", "AAPL",
                            "price", "$214.30",
                            "changePercent", "+1.15%",
                            "volume", "4223000",
                            "sentiment", "Bullish",
                            "risk", "Medium",
                            "recommendation", "Buy on small dips"
                    )
            );
        }
        if ("sales".equals(intent)) {
            return List.of(
                    Map.of(
                            "region", "North",
                            "revenue", "$124K",
                            "target", "$140K",
                            "winRate", "67%",
                            "bestAccount", "Apex Labs",
                            "risk", "Medium"
                    )
            );
        }
        if ("support".equals(intent)) {
            return List.of(
                    Map.of(
                            "ticketId", "INC-1042",
                            "owner", "Tier 2 Support",
                            "priority", "High",
                            "status", "Open",
                            "firstResponse", "4m",
                            "slaRisk", "At risk",
                            "customerHealth", "At risk"
                    )
            );
        }

        return List.of(
                Map.of(
                        "source", "local-mcp-test",
                        "request", prompt,
                        "status", "processed",
                        "summary", "General business request received and structured successfully."
                )
        );
    }
}
