package com.example.agent.registry;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST API for discovering and querying MCP servers.
 * This endpoint allows the UI to connect to MCP servers and discover their tools.
 */
@Path("/api/mcp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MCPDiscoveryResource {
    
    private static final Logger LOGGER = Logger.getLogger(MCPDiscoveryResource.class.getName());
    
    /**
     * Query an MCP server and discover its tools
     * 
     * Request body:
     * {
     *   "url": "http://localhost:3000/mcp",
     *   "transport": "sse"  // or "stdio"
     * }
     */
    @POST
    @Path("/discover")
    public Response discoverTools(Map<String, String> request) {
        String url = request.get("url");
        String transport = request.getOrDefault("transport", "sse");
        
        if (url == null || url.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse("URL is required"))
                .build();
        }
        
        try {
            LOGGER.info("Discovering tools from MCP server: " + url);
            
            // Create MCP client based on transport type
            StandardMCPClient client;
            if ("stdio".equalsIgnoreCase(transport)) {
                // For stdio, expect command and args in request
                String command = request.get("command");
                if (command == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("Command required for stdio transport"))
                        .build();
                }
                client = StandardMCPClient.createStdioClient(command);
            } else {
                // SSE transport - ensure URL points to SSE endpoint
                String sseUrl = url;
                if (!url.endsWith("/sse")) {
                    // If user provides base URL, append /sse
                    sseUrl = url.endsWith("/") ? url + "sse" : url + "/sse";
                    LOGGER.info("Adjusted SSE URL to: " + sseUrl);
                }
                client = StandardMCPClient.createSseClient(sseUrl);
            }
            
            try {
                // Test connection
                if (!client.ping()) {
                    return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(errorResponse("Cannot connect to MCP server"))
                        .build();
                }
                
                // Discover tools
                List<StandardMCPClient.MCPToolInfo> tools = client.listTools();
                
                // Convert to response format
                List<Map<String, Object>> toolsList = tools.stream()
                    .map(tool -> {
                        Map<String, Object> toolMap = new HashMap<>();
                        toolMap.put("name", tool.getName());
                        toolMap.put("description", tool.getDescription());
                        toolMap.put("inputSchema", tool.getInputSchema());
                        return toolMap;
                    })
                    .collect(Collectors.toList());
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("server", client.getServerIdentifier());
                response.put("toolCount", tools.size());
                response.put("tools", toolsList);
                
                LOGGER.info("Discovered " + tools.size() + " tools from " + url);
                return Response.ok(response).build();
                
            } finally {
                client.close();
            }
            
        } catch (Exception e) {
            LOGGER.severe("Failed to discover tools from MCP server: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to connect to MCP server: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Test connection to an MCP server
     * 
     * Request body:
     * {
     *   "url": "http://localhost:3000/mcp"
     * }
     */
    @POST
    @Path("/test")
    public Response testConnection(Map<String, String> request) {
        String url = request.get("url");
        
        if (url == null || url.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse("URL is required"))
                .build();
        }
        
        try {
            LOGGER.info("Testing connection to MCP server: " + url);
            
            try (StandardMCPClient client = StandardMCPClient.createSseClient(url)) {
                boolean connected = client.ping();
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", connected);
                response.put("server", url);
                response.put("message", connected ? "Connected successfully" : "Connection failed");
                
                return Response.ok(response).build();
            }
            
        } catch (Exception e) {
            LOGGER.warning("Connection test failed: " + e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(errorResponse("Connection failed: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Helper method to create error response
     */
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}
