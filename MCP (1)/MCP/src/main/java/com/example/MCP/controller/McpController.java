package com.example.MCP.controller;

import com.example.MCP.service.McpToolService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private final McpToolService toolService;

    public McpController(McpToolService toolService) {
        this.toolService = toolService;
    }

    @PostMapping("/jsonrpc")
    public Map<String, Object> handleJsonRpc(@RequestBody Map<String, Object> request) {
        String method = (String) request.get("method");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        Object id = request.get("id");

        return switch (method) {
            case "initialize" -> toolService.initialize(params, id);
            case "tools/list" -> toolService.listTools(id);
            case "tools/call" -> toolService.callTool(params, id);
            default -> errorResponse("Method not found", id, -32601);
        };
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseEndpoint() {
        SseEmitter emitter = new SseEmitter();
        try {
            // Tell the client where to send JSON‑RPC requests
            emitter.send(SseEmitter.event().name("endpoint").data("/mcp/jsonrpc"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private Map<String, Object> errorResponse(String message, Object id, int code) {
        return Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", code, "message", message),
                "id", id
        );
    }
}