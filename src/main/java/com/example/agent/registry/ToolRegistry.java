package com.example.agent.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.logging.Logger;

/**
 * Simplified tool registry - stores only endpoint info, queries MCP servers for details.
 */
public class ToolRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(ToolRegistry.class.getName());
    private static final String TOOL_KEY_PREFIX = "tool:";
    private static final String TOOL_LIST_KEY = "tools:list";
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final MCPClient mcpClient;
    
    public ToolRegistry() {
        this("localhost", 6379);
    }
    
    public ToolRegistry(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        
        this.jedisPool = new JedisPool(poolConfig, host, port);
        this.objectMapper = new ObjectMapper();
        this.mcpClient = new MCPClient();
    }
    
    /**
     * Register a new MCP tool
     */
    public ToolMetadata registerTool(ToolMetadata metadata) throws Exception {
        if (metadata.getToolId() == null) {
            metadata.setToolId(UUID.randomUUID().toString());
        }
        
        long now = System.currentTimeMillis();
        metadata.setCreatedAt(now);
        metadata.setUpdatedAt(now);
        
        // Validate MCP server is reachable
        boolean healthy = mcpClient.testMCPServer(metadata.getEndpointUrl());
        metadata.setServerHealthy(healthy);
        
        if (!healthy) {
            LOGGER.warning("MCP server not healthy: " + metadata.getEndpointUrl());
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = TOOL_KEY_PREFIX + metadata.getToolId();
            String json = objectMapper.writeValueAsString(metadata);
            
            jedis.set(key, json);
            jedis.sadd(TOOL_LIST_KEY, metadata.getToolId());
            
            LOGGER.info("Registered tool: " + metadata.getName() + " (ID: " + metadata.getToolId() + ")");
            return metadata;
        }
    }
    
    /**
     * Get tool by ID and enrich with MCP server data
     */
    public Optional<ToolMetadata> getTool(String toolId) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = TOOL_KEY_PREFIX + toolId;
            String json = jedis.get(key);
            
            if (json == null) {
                return Optional.empty();
            }
            
            ToolMetadata metadata = objectMapper.readValue(json, ToolMetadata.class);
            
            // Enrich with MCP server data
            enrichWithMCPData(metadata);
            
            return Optional.of(metadata);
        }
    }
    
    /**
     * Get all tools and enrich with MCP data
     */
    public List<ToolMetadata> getAllTools() throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> toolIds = jedis.smembers(TOOL_LIST_KEY);
            List<ToolMetadata> tools = new ArrayList<>();
            
            for (String toolId : toolIds) {
                Optional<ToolMetadata> tool = getTool(toolId);
                tool.ifPresent(tools::add);
            }
            
            return tools;
        }
    }
    
    /**
     * Update tool
     */
    public ToolMetadata updateTool(String toolId, ToolMetadata metadata) throws Exception {
        Optional<ToolMetadata> existing = getTool(toolId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Tool not found: " + toolId);
        }
        
        metadata.setToolId(toolId);
        metadata.setCreatedAt(existing.get().getCreatedAt());
        metadata.setUpdatedAt(System.currentTimeMillis());
        
        // Test MCP server
        boolean healthy = mcpClient.testMCPServer(metadata.getEndpointUrl());
        metadata.setServerHealthy(healthy);
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = TOOL_KEY_PREFIX + toolId;
            String json = objectMapper.writeValueAsString(metadata);
            jedis.set(key, json);
            
            LOGGER.info("Updated tool: " + toolId);
            return metadata;
        }
    }
    
    /**
     * Delete tool
     */
    public boolean deleteTool(String toolId) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = TOOL_KEY_PREFIX + toolId;
            Long deleted = jedis.del(key);
            jedis.srem(TOOL_LIST_KEY, toolId);
            
            if (deleted > 0) {
                LOGGER.info("Deleted tool: " + toolId);
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * Enrich tool metadata by querying its MCP server
     */
    private void enrichWithMCPData(ToolMetadata metadata) {
        try {
            MCPClient.MCPToolInfo info = mcpClient.queryMCPServer(metadata.getEndpointUrl());
            metadata.setMcpTools(info.getTools());
            metadata.setServerHealthy(true);
        } catch (Exception e) {
            LOGGER.warning("Failed to fetch MCP data for " + metadata.getName() + ": " + e.getMessage());
            metadata.setServerHealthy(false);
            metadata.setMcpTools(new ArrayList<>());
        }
    }
    
    /**
     * Close registry
     */
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
