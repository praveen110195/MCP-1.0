package com.mcp.demo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class AiResponseService {

    private final OpenAiService openAiService;

    public AiResponseService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public Map<String, Object> refine(String prompt, Map<String, Object> mcpResponse) {
        String aiSummary = openAiService.generateReply(prompt, mcpResponse);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ai-processed");
        response.put("summary", aiSummary);
        response.put("finalResponse", aiSummary);
        response.put("revisedData", List.of(Map.of(
                "source", "openai",
                "prompt", prompt,
                "rawMcpResponse", mcpResponse,
                "aiSummary", aiSummary
        )));
        return response;
    }
}
