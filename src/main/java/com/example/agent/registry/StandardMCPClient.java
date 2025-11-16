package com.example.agent.registry;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Standard MCP Client implementation using the official Java MCP SDK v0.12.1.
 * Supports both stdio and SSE (Server-Sent Events) transports.
 * 
 * This client conforms to the Model Context Protocol specification and can
 * connect to any standard MCP server.
 * 
 * Based on: https://modelcontextprotocol.io/sdk/java/mcp-client
 */
public class StandardMCPClient implements AutoCloseable {
    
    private static final Logger LOGGER = Logger.getLogger(StandardMCPClient.class.getName());
    
    private final McpSyncClient mcpClient;
    private final TransportType transportType;
    private final String serverIdentifier;
    
    /**
     * Transport types supported by the MCP protocol
     */
    public enum TransportType {
        STDIO,      // Standard input/output (for local processes)
        SSE         // Server-Sent Events (for HTTP connections)
    }
    
    /**
     * Configuration for MCP client connection
     */
    public static class MCPConfig {
        private TransportType transportType;
        private String command;           // For STDIO: command to execute
        private String[] args;            // For STDIO: command arguments
        private String url;               // For SSE: server URL
        private Map<String, String> env;  // Environment variables
        private int timeoutSeconds = 30;  // Connection timeout
        private boolean enableRoots = false;
        private boolean enableSampling = false;
        
        public MCPConfig(TransportType transportType) {
            this.transportType = transportType;
            this.env = new HashMap<>();
        }
        
        public MCPConfig withCommand(String command, String... args) {
            this.command = command;
            this.args = args;
            return this;
        }
        
        public MCPConfig withUrl(String url) {
            this.url = url;
            return this;
        }
        
        public MCPConfig withEnvironment(Map<String, String> env) {
            this.env.putAll(env);
            return this;
        }
        
        public MCPConfig withTimeout(int seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }
        
        public MCPConfig enableRoots(boolean enable) {
            this.enableRoots = enable;
            return this;
        }
        
        public MCPConfig enableSampling(boolean enable) {
            this.enableSampling = enable;
            return this;
        }
        
        public TransportType getTransportType() { return transportType; }
        public String getCommand() { return command; }
        public String[] getArgs() { return args; }
        public String getUrl() { return url; }
        public Map<String, String> getEnv() { return env; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public boolean isEnableRoots() { return enableRoots; }
        public boolean isEnableSampling() { return enableSampling; }
    }
    
    /**
     * Create a new MCP client with the given configuration
     */
    public StandardMCPClient(MCPConfig config) throws IOException {
        this.transportType = config.getTransportType();
        this.serverIdentifier = getServerIdentifier(config);
        
        try {
            // Create transport based on type
            McpClientTransport transport;
            
            switch (config.getTransportType()) {
                case STDIO:
                    if (config.getCommand() == null) {
                        throw new IllegalArgumentException("Command required for STDIO transport");
                    }
                    ServerParameters params = ServerParameters.builder(config.getCommand())
                        .args(config.getArgs() != null ? List.of(config.getArgs()) : List.of())
                        .env(config.getEnv())
                        .build();
                    transport = new StdioClientTransport(params);
                    LOGGER.info("Connecting to MCP server via STDIO: " + config.getCommand());
                    break;
                    
                case SSE:
                    if (config.getUrl() == null) {
                        throw new IllegalArgumentException("URL required for SSE transport");
                    }
                    transport = HttpClientSseClientTransport.builder(config.getUrl()).build();
                    LOGGER.info("Connecting to MCP server via SSE: " + config.getUrl());
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unsupported transport type: " + config.getTransportType());
            }
            
            // Build client capabilities
            McpSchema.ClientCapabilities.Builder capBuilder = McpSchema.ClientCapabilities.builder();
            if (config.isEnableRoots()) {
                capBuilder.roots(true);
            }
            if (config.isEnableSampling()) {
                capBuilder.sampling();
            }
            
            // Build the client
            LOGGER.info("Building MCP client with timeout: " + config.getTimeoutSeconds() + "s");
            this.mcpClient = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .capabilities(capBuilder.build())
                .build();
            
            // Initialize the connection
            LOGGER.info("Initializing MCP client connection to: " + serverIdentifier);
            try {
                mcpClient.initialize();
                LOGGER.info("Successfully connected to MCP server: " + serverIdentifier);
            } catch (Exception initEx) {
                LOGGER.severe("Initialization failed: " + initEx.getClass().getName() + ": " + initEx.getMessage());
                if (initEx.getCause() != null) {
                    LOGGER.severe("  Caused by: " + initEx.getCause().getClass().getName() + ": " + initEx.getCause().getMessage());
                }
                throw initEx;
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize MCP client", e);
            throw new IOException("Failed to connect to MCP server: " + e.getMessage(), e);
        }
    }
    
    /**
     * List all available tools from the MCP server
     */
    public List<MCPToolInfo> listTools() throws Exception {
        try {
            McpSchema.ListToolsResult result = mcpClient.listTools();
            
            return result.tools().stream()
                .map(this::convertToToolInfo)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to list tools from MCP server", e);
            throw new Exception("Failed to retrieve tools: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get information about a specific tool
     */
    public MCPToolInfo getTool(String toolName) throws Exception {
        List<MCPToolInfo> tools = listTools();
        return tools.stream()
            .filter(t -> t.getName().equals(toolName))
            .findFirst()
            .orElseThrow(() -> new Exception("Tool not found: " + toolName));
    }
    
    /**
     * Call a tool with the given arguments
     */
    public ToolCallResult callTool(String toolName, Map<String, Object> arguments) throws Exception {
        try {
            McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
                .name(toolName)
                .arguments(arguments)
                .build();
                
            McpSchema.CallToolResult result = mcpClient.callTool(request);
            
            return new ToolCallResult(
                result.content(),
                result.isError(),
                serverIdentifier
            );
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to call tool: " + toolName, e);
            throw new Exception("Tool call failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test if the MCP server is responsive
     */
    public boolean ping() {
        try {
            mcpClient.listTools();
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "MCP server ping failed", e);
            return false;
        }
    }
    
    /**
     * Get server information
     */
    public String getServerIdentifier() {
        return serverIdentifier;
    }
    
    /**
     * Get the underlying sync client for advanced operations
     */
    public McpSyncClient getClient() {
        return mcpClient;
    }
    
    /**
     * Close the MCP client connection
     */
    @Override
    public void close() {
        try {
            if (mcpClient != null) {
                mcpClient.close();
                LOGGER.info("Closed MCP client connection: " + serverIdentifier);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing MCP client", e);
        }
    }
    
    // Helper methods
    
    private String getServerIdentifier(MCPConfig config) {
        switch (config.getTransportType()) {
            case STDIO:
                return "stdio://" + config.getCommand();
            case SSE:
                return config.getUrl();
            default:
                return "unknown";
        }
    }
    
    private MCPToolInfo convertToToolInfo(McpSchema.Tool tool) {
        MCPToolInfo info = new MCPToolInfo();
        info.setName(tool.name());
        info.setDescription(tool.description());
        // Convert JsonSchema to Map - for simplicity, store as generic object
        info.setInputSchema(tool.inputSchema() != null ? Map.of("schema", tool.inputSchema()) : new HashMap<>());
        return info;
    }
    
    /**
     * Information about an MCP tool
     */
    public static class MCPToolInfo {
        private String name;
        private String description;
        private Map<String, Object> inputSchema;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Map<String, Object> getInputSchema() { return inputSchema; }
        public void setInputSchema(Map<String, Object> inputSchema) { this.inputSchema = inputSchema; }
    }
    
    /**
     * Result from calling an MCP tool
     */
    public static class ToolCallResult {
        private final List<McpSchema.Content> content;
        private final boolean isError;
        private final String serverIdentifier;
        
        public ToolCallResult(List<McpSchema.Content> content, boolean isError, String serverIdentifier) {
            this.content = content;
            this.isError = isError;
            this.serverIdentifier = serverIdentifier;
        }
        
        public List<McpSchema.Content> getContent() { return content; }
        public boolean isError() { return isError; }
        public String getServerIdentifier() { return serverIdentifier; }
    }
    
    /**
     * Factory method to create a stdio-based MCP client
     */
    public static StandardMCPClient createStdioClient(String command, String... args) throws IOException {
        MCPConfig config = new MCPConfig(TransportType.STDIO)
            .withCommand(command, args);
        return new StandardMCPClient(config);
    }
    
    /**
     * Factory method to create an SSE-based MCP client
     */
    public static StandardMCPClient createSseClient(String url) throws IOException {
        MCPConfig config = new MCPConfig(TransportType.SSE)
            .withUrl(url);
        return new StandardMCPClient(config);
    }
}
