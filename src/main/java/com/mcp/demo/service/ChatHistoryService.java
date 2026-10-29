package com.mcp.demo.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ChatHistoryService {

    private final Deque<Map<String, Object>> history = new ArrayDeque<>();

    public void add(String requestId, String prompt, String rawMcpResponse, String finalResponse) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("requestId", requestId);
        entry.put("prompt", prompt);
        entry.put("timestamp", Instant.now().toString());
        entry.put("rawMcpResponse", rawMcpResponse);
        entry.put("finalResponse", finalResponse);
        history.addFirst(entry);
        while (history.size() > 10) {
            history.removeLast();
        }
    }

    public List<Map<String, Object>> getHistory() {
        return new ArrayList<>(history);
    }
}
