package com.mcp.demo.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenAiService {

    private final RestTemplate restTemplate;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.organization:}")
    private String organization;

    public OpenAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public String generateReply(String prompt, Map<String, Object> mcpResponse) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is missing. Set the environment variable before running the app.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        if (model != null && (model.contains("gpt-5") || model.contains("gpt-6") || model.contains("o1") || model.contains("o3"))) {
            payload.put("reasoning_effort", "none");
        }
        payload.put("temperature", 0.2);
        payload.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content",
                                "You are a helpful AI assistant. The user request is based on data returned by an MCP tool. " +
                                "Interpret the MCP result, provide concise analysis, and present a polished response."
                ),
                Map.of(
                        "role", "user",
                        "content",
                                "User request: " + prompt + "\n\nMCP payload: " + mcpResponse
                )
        ));

        payload.put("tools", List.of(
                Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "summarize_mcp_data",
                                "description", "Summarize structured MCP data for the user.",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "summary", Map.of("type", "string")
                                        ),
                                        "required", List.of("summary")
                                )
                        )
                )
        ));
        payload.put("tool_choice", "auto");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        if (organization != null && !organization.isBlank()) {
            headers.set("OpenAI-Organization", organization);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("https://api.openai.com/v1/chat/completions", request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("OpenAI request failed: " + response.getStatusCode());
        }

        Map<String, Object> body = response.getBody();
        List<?> choices = (List<?>) body.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("OpenAI returned no choices.");
        }

        Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
        if (message != null && message.get("tool_calls") != null) {
            List<?> toolCalls = (List<?>) message.get("tool_calls");
            if (!toolCalls.isEmpty()) {
                Map<?, ?> toolCall = (Map<?, ?>) toolCalls.get(0);
                Map<?, ?> function = (Map<?, ?>) toolCall.get("function");
                String functionName = String.valueOf(function.get("name"));
                if ("summarize_mcp_data".equals(functionName)) {
                    return buildToolSummary(prompt, mcpResponse);
                }
            }
        }

        if (message == null || message.get("content") == null) {
            throw new IllegalStateException("OpenAI returned an empty message content.");
        }

        return String.valueOf(message.get("content")).trim();
    }

    private String buildToolSummary(String prompt, Map<String, Object> mcpResponse) {
        StringBuilder summary = new StringBuilder();
        summary.append("AI summary for: ").append(prompt).append("\n\n");

        Object data = mcpResponse.get("data");
        if (data instanceof List<?> items) {
            List<String> citySummaries = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    Object cityValue = map.get("city");
                    Object tempValue = map.get("temperature");
                    Object conditionValue = map.get("condition");
                    String city = cityValue == null ? "Location" : String.valueOf(cityValue);
                    String temp = tempValue == null ? "N/A" : String.valueOf(tempValue);
                    String condition = conditionValue == null ? "normal" : String.valueOf(conditionValue);
                    citySummaries.add(city + " is " + condition.toLowerCase() + " at " + temp + ".");
                }
            }
            if (!citySummaries.isEmpty()) {
                summary.append(String.join(" ", citySummaries));
                return summary.toString();
            }
        }

        summary.append("MCP data was received and reviewed successfully.");
        return summary.toString();
    }
}
