package com.example.agent.registry;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for A2A agent management
 */
@Path("/api/a2a-agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class A2AAgentResource {
    
    private static final Logger logger = LoggerFactory.getLogger(A2AAgentResource.class);
    private final A2AClientService clientService;
    
    public A2AAgentResource(A2AClientService clientService) {
        this.clientService = clientService;
    }
    
    /**
     * Register a new A2A agent
     */
    @POST
    public Response registerAgent(Map<String, String> request) {
        String agentUrl = request.get("url");
        
        if (agentUrl == null || agentUrl.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Agent URL is required"))
                    .build();
        }
        
        try {
            logger.info("Registering A2A agent from URL: {}", agentUrl);
            A2AAgentMetadata metadata = clientService.connectToAgent(agentUrl);
            
            return Response.ok(metadata).build();
            
        } catch (Exception e) {
            logger.error("Failed to register A2A agent: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Get all registered A2A agents
     */
    @GET
    public Response getAllAgents() {
        try {
            List<A2AAgentMetadata> agents = clientService.getAllAgents();
            return Response.ok(agents).build();
        } catch (Exception e) {
            logger.error("Failed to get A2A agents: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Get a specific A2A agent by ID
     */
    @GET
    @Path("/{agentId}")
    public Response getAgent(@PathParam("agentId") String agentId) {
        try {
            A2AAgentMetadata metadata = clientService.getAgent(agentId);
            
            if (metadata == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Agent not found"))
                        .build();
            }
            
            return Response.ok(metadata).build();
            
        } catch (Exception e) {
            logger.error("Failed to get A2A agent {}: {}", agentId, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Disconnect from an A2A agent
     */
    @DELETE
    @Path("/{agentId}")
    public Response disconnectAgent(@PathParam("agentId") String agentId) {
        try {
            boolean success = clientService.disconnectAgent(agentId);
            
            if (success) {
                return Response.ok(Map.of("message", "Agent disconnected successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Agent not found"))
                        .build();
            }
            
        } catch (Exception e) {
            logger.error("Failed to disconnect A2A agent {}: {}", agentId, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Test connection to an A2A agent
     */
    @POST
    @Path("/test")
    public Response testConnection(Map<String, String> request) {
        String agentUrl = request.get("url");
        
        if (agentUrl == null || agentUrl.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Agent URL is required"))
                    .build();
        }
        
        try {
            Map<String, Object> result = clientService.testConnection(agentUrl);
            return Response.ok(result).build();
            
        } catch (Exception e) {
            logger.error("Failed to test A2A agent connection: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Get status of A2A agent registry
     */
    @GET
    @Path("/status")
    public Response getStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            List<A2AAgentMetadata> agents = clientService.getAllAgents();
            
            status.put("totalAgents", agents.size());
            status.put("connectedAgents", agents.stream().filter(a -> "connected".equals(a.getStatus())).count());
            status.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(status).build();
            
        } catch (Exception e) {
            logger.error("Failed to get A2A agent status: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Send a message to an A2A agent
     */
    @POST
    @Path("/{agentId}/message")
    public Response sendMessage(@PathParam("agentId") String agentId, Map<String, String> request) {
        String message = request.get("message");
        
        if (message == null || message.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Message is required"))
                    .build();
        }
        
        try {
            logger.info("Sending message to agent {}: {}", agentId, message);
            long startTime = System.currentTimeMillis();
            
            String agentResponse = clientService.sendMessage(agentId, message);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("response", agentResponse);
            
            Map<String, Object> details = new HashMap<>();
            details.put("execution_time", executionTime);
            response.put("details", details);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Failed to send message to agent {}: {}", agentId, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
