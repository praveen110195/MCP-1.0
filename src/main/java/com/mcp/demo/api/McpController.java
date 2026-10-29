package com.mcp.demo.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mcp.demo.service.AiResponseService;
import com.mcp.demo.service.ChatHistoryService;
import com.mcp.demo.service.McpToolService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class McpController {

    private final McpToolService mcpToolService;
    private final AiResponseService aiResponseService;
    private final ChatHistoryService chatHistoryService;

    public McpController(McpToolService mcpToolService, AiResponseService aiResponseService, ChatHistoryService chatHistoryService) {
        this.mcpToolService = mcpToolService;
        this.aiResponseService = aiResponseService;
        this.chatHistoryService = chatHistoryService;
    }

    @GetMapping("/mcp/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "mcp-ai-studio");
    }

    @GetMapping("/chat/history")
    public List<Map<String, Object>> history() {
        return chatHistoryService.getHistory();
    }

    @PostMapping("/mcp/execute")
    public ResponseEntity<Map<String, Object>> execute(@RequestBody ToolRequest request) {
        String prompt = request.prompt() == null ? "" : request.prompt();
        String context = request.context() == null ? "Default workspace context" : request.context();
        String requestId = UUID.randomUUID().toString();

        try {
            Map<String, Object> rawMcpResponse = mcpToolService.execute(prompt, context);
            Map<String, Object> aiModifiedResponse = aiResponseService.refine(prompt, rawMcpResponse);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("rawMcpResponse", rawMcpResponse);
            response.put("aiModifiedResponse", aiModifiedResponse);
            response.put("finalResponse", aiModifiedResponse.get("finalResponse"));

            chatHistoryService.add(requestId, prompt, String.valueOf(rawMcpResponse), String.valueOf(aiModifiedResponse.get("finalResponse")));
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            Map<String, Object> error = new LinkedHashMap<>();
            Map<String, Object> rawMcpResponse = mcpToolService.execute(prompt, context);
            error.put("requestId", requestId);
            error.put("status", "config_error");
            error.put("rawMcpResponse", rawMcpResponse);
            error.put("aiModifiedResponse", Map.of(
                    "status", "blocked",
                    "summary", ex.getMessage(),
                    "finalResponse", ex.getMessage()
            ));
            error.put("error", ex.getMessage());
            error.put("finalResponse", ex.getMessage());

            chatHistoryService.add(requestId, prompt, String.valueOf(rawMcpResponse), ex.getMessage());
            return ResponseEntity.ok(error);
        } catch (Exception ex) {
            Map<String, Object> error = new LinkedHashMap<>();
            Map<String, Object> rawMcpResponse = mcpToolService.execute(prompt, context);
            error.put("requestId", requestId);
            error.put("status", "error");
            error.put("rawMcpResponse", rawMcpResponse);
            error.put("error", ex.getMessage());
            error.put("finalResponse", ex.getMessage());
            chatHistoryService.add(requestId, prompt, String.valueOf(rawMcpResponse), ex.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
