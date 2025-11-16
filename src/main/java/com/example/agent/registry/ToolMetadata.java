package com.example.agent.registry;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simplified metadata for MCP tools.
 * Only stores registration info - full tool details fetched from MCP server.
 */
public class ToolMetadata {
    
    @JsonProperty("tool_id")
    private String toolId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("endpoint_url")
    private String endpointUrl;
    
    @JsonProperty("created_at")
    private Long createdAt;
    
    @JsonProperty("updated_at")
    private Long updatedAt;
    
    // Transient field - populated by querying MCP server
    @JsonProperty("mcp_tools")
    private java.util.List<java.util.Map<String, Object>> mcpTools;
    
    @JsonProperty("server_healthy")
    private Boolean serverHealthy;
    
    public ToolMetadata() {
    }
    
    // Getters and Setters
    public String getToolId() {
        return toolId;
    }
    
    public void setToolId(String toolId) {
        this.toolId = toolId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getEndpointUrl() {
        return endpointUrl;
    }
    
    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public java.util.List<java.util.Map<String, Object>> getMcpTools() {
        return mcpTools;
    }
    
    public void setMcpTools(java.util.List<java.util.Map<String, Object>> mcpTools) {
        this.mcpTools = mcpTools;
    }
    
    public Boolean getServerHealthy() {
        return serverHealthy;
    }
    
    public void setServerHealthy(Boolean serverHealthy) {
        this.serverHealthy = serverHealthy;
    }
}
