package com.example.MCP.model;



import java.util.List;

public class McpResponse {
    private String jsonrpc;
    private Object id;
    private McpResult result;
    private McpError error;

    // Default Constructor
    public McpResponse() {
    }

    // Full Constructor
    public McpResponse(String jsonrpc, Object id, McpResult result, McpError error) {
        this.jsonrpc = jsonrpc;
        this.id = id;
        this.result = result;
        this.error = error;
    }

    // Helper method for a successful JSON-RPC tool response
    public static McpResponse success(Object id, String textPayload) {
        McpContent content = new McpContent("text", textPayload);
        McpResult result = new McpResult(List.of(content));
        return new McpResponse("2.0", id, result, null);
    }

    // Helper method for a normalized JSON-RPC error response
    public static McpResponse failure(Object id, int errorCode, String errorMessage) {
        McpError error = new McpError(errorCode, errorMessage);
        return new McpResponse("2.0", id, null, error);
    }

    // Getters and Setters
    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }

    public Object getId() { return id; }
    public void setId(Object id) { this.id = id; }

    public McpResult getResult() { return result; }
    public void setResult(McpResult result) { this.result = result; }

    public McpError getError() { return error; }
    public void setError(McpError error) { this.error = error; }

    // Inner Class: McpResult
    public static class McpResult {
        private List<McpContent> content;

        public McpResult() {}
        public McpResult(List<McpContent> content) { this.content = content; }

        public List<McpContent> getContent() { return content; }
        public void setContent(List<McpContent> content) { this.content = content; }
    }

    // Inner Class: McpContent
    public static class McpContent {
        private String type;
        private String text;

        public McpContent() {}
        public McpContent(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    // Inner Class: McpError
    public static class McpError {
        private int code;
        private String message;

        public McpError() {}
        public McpError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}