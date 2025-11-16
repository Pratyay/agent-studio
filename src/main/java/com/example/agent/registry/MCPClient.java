package com.example.agent.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Client for querying MCP (Model Context Protocol) servers.
 * Fetches tool information from MCP server endpoints.
 */
public class MCPClient {
    
    private static final Logger LOGGER = Logger.getLogger(MCPClient.class.getName());
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public MCPClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Query an MCP server to get tool information
     * Now uses StandardMCPClient via local discovery endpoint
     */
    public MCPToolInfo queryMCPServer(String endpoint) throws Exception {
        try {
            LOGGER.info("Querying MCP server: " + endpoint);
            
            // Use StandardMCPClient through discovery endpoint
            // This ensures we use proper SSE transport
            String discoveryUrl = "http://localhost:8080/api/mcp/discover";
            
            // Prepare request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("url", endpoint);
            requestBody.put("transport", "sse");
            
            String requestJson = objectMapper.writeValueAsString(requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(discoveryUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(30))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new Exception("Discovery endpoint returned status: " + response.statusCode());
            }
            
            // Parse discovery response
            Map<String, Object> discoveryResponse = objectMapper.readValue(response.body(), Map.class);
            
            if (Boolean.FALSE.equals(discoveryResponse.get("success"))) {
                throw new Exception("Discovery failed: " + discoveryResponse.get("error"));
            }
            
            return parseDiscoveryResponse(discoveryResponse);
            
        } catch (Exception e) {
            LOGGER.warning("Failed to query MCP server at " + endpoint + ": " + e.getMessage());
            throw new Exception("Failed to connect to MCP server: " + e.getMessage());
        }
    }
    
    /**
     * Test if an MCP server is reachable
     */
    public boolean testMCPServer(String endpoint) {
        try {
            String testUrl = endpoint.endsWith("/") ? endpoint + "health" : endpoint + "/health";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(testUrl))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            LOGGER.fine("MCP server health check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Parse discovery endpoint response
     */
    private MCPToolInfo parseDiscoveryResponse(Map<String, Object> response) {
        MCPToolInfo info = new MCPToolInfo();
        
        // Extract tools list from discovery response
        Object toolsObj = response.get("tools");
        if (toolsObj instanceof java.util.List) {
            info.setTools((java.util.List<Map<String, Object>>) toolsObj);
        }
        
        // Extract server identifier
        if (response.containsKey("server")) {
            info.setServerName((String) response.get("server"));
        }
        
        return info;
    }
    
    /**
     * Container for MCP tool information
     */
    public static class MCPToolInfo {
        private java.util.List<Map<String, Object>> tools;
        private String serverName;
        private String serverVersion;
        
        public MCPToolInfo() {
            this.tools = new java.util.ArrayList<>();
        }
        
        public java.util.List<Map<String, Object>> getTools() {
            return tools;
        }
        
        public void setTools(java.util.List<Map<String, Object>> tools) {
            this.tools = tools;
        }
        
        public String getServerName() {
            return serverName;
        }
        
        public void setServerName(String serverName) {
            this.serverName = serverName;
        }
        
        public String getServerVersion() {
            return serverVersion;
        }
        
        public void setServerVersion(String serverVersion) {
            this.serverVersion = serverVersion;
        }
        
        public int getToolCount() {
            return tools != null ? tools.size() : 0;
        }
    }
}
