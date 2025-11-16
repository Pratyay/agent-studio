package com.example.agent.registry;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API resource for agent registry operations.
 * Provides endpoints for registering, discovering, and managing agents.
 */
@Path("/api/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentRegistryResource {
    
    private final AgentRegistry registry;
    private final DynamicAgentLoader loader;
    private final RegistryAwareRouter router;
    
    public AgentRegistryResource(AgentRegistry registry, 
                                 DynamicAgentLoader loader,
                                 RegistryAwareRouter router) {
        this.registry = registry;
        this.loader = loader;
        this.router = router;
    }
    
    /**
     * Register a new agent
     */
    @POST
    public Response registerAgent(AgentMetadata metadata) {
        try {
            AgentMetadata registered = registry.registerAgent(metadata);
            return Response.status(Response.Status.CREATED).entity(registered).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse("Failed to register agent: " + e.getMessage())).build();
        }
    }
    
    /**
     * Get all agents
     */
    @GET
    public Response getAllAgents() {
        try {
            List<AgentMetadata> agents = registry.getAllAgents();
            return Response.ok(agents).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to retrieve agents: " + e.getMessage())).build();
        }
    }
    
    /**
     * Get agent by ID
     */
    @GET
    @Path("/{agentId}")
    public Response getAgent(@PathParam("agentId") String agentId) {
        try {
            Optional<AgentMetadata> agent = registry.getAgent(agentId);
            if (agent.isPresent()) {
                return Response.ok(agent.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse("Agent not found: " + agentId)).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to retrieve agent: " + e.getMessage())).build();
        }
    }
    
    /**
     * Update agent
     */
    @PUT
    @Path("/{agentId}")
    public Response updateAgent(@PathParam("agentId") String agentId, 
                                AgentMetadata metadata) {
        try {
            AgentMetadata updated = registry.updateAgent(agentId, metadata);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse(e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to update agent: " + e.getMessage())).build();
        }
    }
    
    /**
     * Delete agent
     */
    @DELETE
    @Path("/{agentId}")
    public Response deleteAgent(@PathParam("agentId") String agentId) {
        try {
            boolean deleted = registry.unregisterAgent(agentId);
            if (deleted) {
                return Response.ok(successResponse("Agent deleted successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse("Agent not found: " + agentId)).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to delete agent: " + e.getMessage())).build();
        }
    }
    
    /**
     * Find agents by capability
     */
    @GET
    @Path("/capability/{capability}")
    public Response findByCapability(@PathParam("capability") String capability) {
        try {
            List<AgentMetadata> agents = registry.findAgentsByCapability(capability);
            return Response.ok(agents).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to search agents: " + e.getMessage())).build();
        }
    }
    
    /**
     * Load an agent
     */
    @POST
    @Path("/{agentId}/load")
    public Response loadAgent(@PathParam("agentId") String agentId) {
        try {
            loader.loadAgent(agentId);
            return Response.ok(successResponse("Agent loaded successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to load agent: " + e.getMessage())).build();
        }
    }
    
    /**
     * Unload an agent
     */
    @POST
    @Path("/{agentId}/unload")
    public Response unloadAgent(@PathParam("agentId") String agentId) {
        try {
            boolean unloaded = loader.unloadAgent(agentId);
            if (unloaded) {
                return Response.ok(successResponse("Agent unloaded successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse("Agent not loaded: " + agentId)).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to unload agent: " + e.getMessage())).build();
        }
    }
    
    /**
     * Get all available agents with load status
     */
    @GET
    @Path("/available")
    public Response getAvailableAgents() {
        try {
            List<RegistryAwareRouter.AgentInfo> agents = router.listAvailableAgents();
            return Response.ok(agents).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to retrieve available agents: " + e.getMessage())).build();
        }
    }
    
    /**
     * Get registry status
     */
    @GET
    @Path("/status")
    public Response getStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("total_agents", registry.getAllAgents().size());
            status.put("loaded_agents", loader.getAllLoadedAgents().size());
            status.put("redis_connected", true);
            return Response.ok(status).build();
        } catch (Exception e) {
            Map<String, Object> status = new HashMap<>();
            status.put("error", e.getMessage());
            status.put("redis_connected", false);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(status).build();
        }
    }
    
    /**
     * Helper method to create error response
     */
    private Map<String, String> errorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
    
    /**
     * Helper method to create success response
     */
    private Map<String, String> successResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return response;
    }
}
