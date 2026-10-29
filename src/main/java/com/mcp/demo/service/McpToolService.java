package com.mcp.demo.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private static final Map<String, String> CITY_COUNTRY = Map.ofEntries(
            Map.entry("bangalore", "India"),
            Map.entry("bengaluru", "India"),
            Map.entry("singapore", "Singapore"),
            Map.entry("mumbai", "India"),
            Map.entry("delhi", "India"),
            Map.entry("london", "United Kingdom"),
            Map.entry("paris", "France"),
            Map.entry("new york", "United States"),
            Map.entry("tokyo", "Japan"),
            Map.entry("dubai", "UAE"),
            Map.entry("berlin", "Germany"),
            Map.entry("sydney", "Australia")
    );

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
                    System.out.println("MCP remote call successful. Response received from " + response.getBody());
                    return response.getBody();
                }
            } catch (Exception e) {
                log.warn("MCP remote call failed, falling back to local mock data. Cause: {}", e.getMessage());
            }
        }

        return buildSmartMockPayload(prompt, context);
    }

    private Map<String, Object> buildSmartMockPayload(String prompt, String context) {
        String normalizedPrompt = (prompt == null ? "" : prompt).toLowerCase(Locale.ROOT);
        String intent = detectIntent(normalizedPrompt);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tool", "smart_dynamic_tool");
        payload.put("status", "success");
        payload.put("request", prompt);
        payload.put("context", context);
        payload.put("source", "mcp-local-smart");
        payload.put("intent", intent);
        payload.put("entities", extractEntities(prompt));
        payload.put("summary", buildSummary(intent, prompt));
        payload.put("recommendations", buildRecommendations(intent));
        payload.put("data", buildDataForIntent(intent, prompt));
        payload.put("metadata", Map.of(
                "generatedAt", java.time.Instant.now().toString(),
                "responseMode", "smart-local-fallback",
                "quality", "dynamic"
        ));
        return payload;
    }

    private String detectIntent(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "general";
        }
        if (prompt.contains("travel") || prompt.contains("flight") || prompt.contains("hotel") || prompt.contains("trip") || prompt.contains("visit") || prompt.contains("weather")) {
            return "travel";
        }
        if (prompt.contains("stock") || prompt.contains("market") || prompt.contains("share") || prompt.contains("portfolio") || prompt.contains("invest")) {
            return "stock";
        }
        if (prompt.contains("sales") || prompt.contains("revenue") || prompt.contains("pipeline") || prompt.contains("quota") || prompt.contains("customer")) {
            return "sales";
        }
        if (prompt.contains("support") || prompt.contains("ticket") || prompt.contains("issue") || prompt.contains("incident") || prompt.contains("customer service")) {
            return "support";
        }
        return "general";
    }

    private List<String> extractEntities(String prompt) {
        List<String> entities = new ArrayList<>();
        if (prompt == null || prompt.isBlank()) {
            return entities;
        }

        for (String city : CITY_COUNTRY.keySet()) {
            if (prompt.toLowerCase(Locale.ROOT).contains(city)) {
                entities.add(capitalize(city));
            }
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[A-Z][A-Za-z0-9.-]+(?:\\.[A-Z][A-Za-z0-9.-]+)?");
        java.util.regex.Matcher matcher = pattern.matcher(prompt);
        while (matcher.find()) {
            entities.add(matcher.group());
        }

        if (entities.isEmpty()) {
            entities.add("General Business Context");
        }

        return entities.stream().distinct().limit(8).toList();
    }

    private String buildSummary(String intent, String prompt) {
        if ("travel".equals(intent)) {
            return "Travel intent detected: the system derived destination context and generated operational travel insights for the requested trip.";
        }
        if ("stock".equals(intent)) {
            return "Market intent detected: the system modeled a portfolio snapshot with price movement and risk signals for the requested investment query.";
        }
        if ("sales".equals(intent)) {
            return "Sales intent detected: the system produced a pipeline and revenue snapshot aligned with the user’s commercial request.";
        }
        if ("support".equals(intent)) {
            return "Support intent detected: the system structured a ticket-resolution and service quality view for the requested issue.";
        }
        return "General business intent detected: the system returned a structured operational summary based on the prompt and context.";
    }

    private List<String> buildRecommendations(String intent) {
        if ("travel".equals(intent)) {
            return List.of(
                    "Check weather and connectivity before departure.",
                    "Reconfirm hotel and transit reservations.",
                    "Share the plan with stakeholders.");
        }
        if ("stock".equals(intent)) {
            return List.of(
                    "Review price momentum against the weekly trend.",
                    "Cross-check volatility and market sentiment.",
                    "Set alerts for breakout and breakdown levels.");
        }
        if ("sales".equals(intent)) {
            return List.of(
                    "Prioritize high-probability deals in the pipeline.",
                    "Review discount variance against target margin.",
                    "Schedule follow-up meetings with stalled accounts.");
        }
        if ("support".equals(intent)) {
            return List.of(
                    "Group tickets by impact and SLA urgency.",
                    "Assign the highest-risk incidents to senior responders.",
                    "Publish a customer update once a root-cause review is complete.");
        }
        return List.of(
                "Validate the source data before publishing insights.",
                "Use this response as a working summary, not a final decision.",
                "Review the operational context for follow-up actions.");
    }

    private List<Map<String, Object>> buildDataForIntent(String intent, String prompt) {
        if ("travel".equals(intent)) {
            return buildTravelData(prompt);
        }
        if ("stock".equals(intent)) {
            return buildStockData(prompt);
        }
        if ("sales".equals(intent)) {
            return buildSalesData(prompt);
        }
        if ("support".equals(intent)) {
            return buildSupportData(prompt);
        }
        return buildGeneralData(prompt);
    }

    private List<Map<String, Object>> buildTravelData(String prompt) {
        List<String> cities = extractCities(prompt);
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < cities.size(); i++) {
            String city = cities.get(i);
            String country = CITY_COUNTRY.getOrDefault(city.toLowerCase(Locale.ROOT), "Local Market");
            data.add(Map.of(
                    "city", city,
                    "country", country,
                    "temperature", String.valueOf(18 + ((i * 7) % 20)) + "°C",
                    "condition", nextCondition(i),
                    "humidity", String.valueOf(52 + ((i * 13) % 32)) + "%",
                    "flightStatus", i % 2 == 0 ? "On time" : "Delayed by 20 min",
                    "hotelReadiness", i % 2 == 0 ? "Ready" : "Needs review",
                    "recommendation", i % 2 == 0 ? "Good for outdoor plans" : "Keep indoor backup plan"
            ));
        }
        if (data.isEmpty()) {
            data.add(Map.of(
                    "city", "Bangalore",
                    "country", "India",
                    "temperature", "28°C",
                    "condition", "Cloudy",
                    "humidity", "68%",
                    "flightStatus", "On time",
                    "hotelReadiness", "Ready",
                    "recommendation", "Good for a relaxed itinerary"
            ));
        }
        return data;
    }

    private List<Map<String, Object>> buildStockData(String prompt) {
        List<String> symbols = List.of("AAPL", "MSFT", "NVDA", "TSLA");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);
            rows.add(Map.of(
                    "symbol", symbol,
                    "price", String.format(Locale.US, "$%.2f", 110 + (i * 27.5)),
                    "changePercent", String.format(Locale.US, "+%.2f%%", 0.8 + (i * 0.6)),
                    "volume", String.valueOf(2000000 + (i * 500000)),
                    "sentiment", i % 2 == 0 ? "Bullish" : "Neutral",
                    "risk", i % 2 == 0 ? "Medium" : "Low",
                    "recommendation", i % 2 == 0 ? "Buy on small dips" : "Hold for trend confirmation"
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildSalesData(String prompt) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(Map.of(
                "region", "North",
                "revenue", "$124K",
                "target", "$140K",
                "winRate", "67%",
                "bestAccount", "Apex Labs",
                "risk", "Medium"));
        rows.add(Map.of(
                "region", "West",
                "revenue", "$98K",
                "target", "$110K",
                "winRate", "58%",
                "bestAccount", "Orbit Systems",
                "risk", "High"));
        rows.add(Map.of(
                "region", "South",
                "revenue", "$116K",
                "target", "$130K",
                "winRate", "61%",
                "bestAccount", "Summit Retail",
                "risk", "Medium"));
        return rows;
    }

    private List<Map<String, Object>> buildSupportData(String prompt) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(Map.of(
                "ticketId", "INC-1042",
                "status", "Open",
                "priority", "High",
                "firstResponse", "4m",
                "slaRisk", "At risk",
                "customerHealth", "At risk",
                "owner", "Tier 2 Support"));
        rows.add(Map.of(
                "ticketId", "INC-1049",
                "status", "Monitoring",
                "priority", "Medium",
                "firstResponse", "12m",
                "slaRisk", "Healthy",
                "customerHealth", "Stable",
                "owner", "Tier 1 Support"));
        return rows;
    }

    private List<Map<String, Object>> buildGeneralData(String prompt) {
        return List.of(Map.of(
                "label", "Operational summary",
                "value", "The prompt was analyzed for business pattern and structured insights were generated.",
                "confidence", "High",
                "source", "local-smart-generator"
        ));
    }

    private List<String> extractCities(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return List.of("Bangalore", "Singapore");
        }

        String normalized = prompt.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        Set<String> keys = CITY_COUNTRY.keySet();
        for (String city : keys) {
            if (normalized.contains(city)) {
                matches.add(capitalize(city));
            }
        }

        if (!matches.isEmpty()) {
            return matches;
        }

        String[] tokens = prompt.split("[^A-Za-z]+");
        List<String> candidates = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 3 && Character.isUpperCase(token.charAt(0))) {
                candidates.add(token);
            }
        }
        if (!candidates.isEmpty()) {
            return candidates.stream().distinct().limit(3).toList();
        }

        return List.of("Bangalore", "Singapore");
    }

    private String nextCondition(int index) {
        String[] conditions = {"Sunny", "Cloudy", "Rainy", "Windy", "Clear"};
        return conditions[index % conditions.length];
    }

    private String capitalize(String value) {
        String[] parts = value.split(" ");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
}
