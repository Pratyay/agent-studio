package com.example.agent.registry;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Simplified REST API for tool registry.
 */
@Path("/api/tools")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ToolRegistryResource {
    
    private final ToolRegistry registry;
    
    public ToolRegistryResource(ToolRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * Register a new MCP tool
     */
    @POST
    public Response registerTool(ToolMetadata metadata) {
        try {
            ToolMetadata registered = registry.registerTool(metadata);
            return Response.status(Response.Status.CREATED).entity(registered).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse("Failed to register tool: " + e.getMessage())).build();
        }
    }
    
    /**
     * Get all tools (with MCP data enriched)
     */
    @GET
    public Response getAllTools() {
        try {
            List<ToolMetadata> tools = registry.getAllTools();
            return Response.ok(tools).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to retrieve tools: " + e.getMessage())).build();
        }
    }
    
    /**
     * Get tool by ID (with MCP data enriched)
     */
    @GET
    @Path("/{toolId}")
    public Response getTool(@PathParam("toolId") String toolId) {
        try {
            Optional<ToolMetadata> tool = registry.getTool(toolId);
            if (tool.isPresent()) {
                return Response.ok(tool.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse("Tool not found: " + toolId)).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to retrieve tool: " + e.getMessage())).build();
        }
    }
    
    /**
     * Update tool
     */
    @PUT
    @Path("/{toolId}")
    public Response updateTool(@PathParam("toolId") String toolId, 
                               ToolMetadata metadata) {
        try {
            ToolMetadata updated = registry.updateTool(toolId, metadata);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse(e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to update tool: " + e.getMessage())).build();
        }
    }
    
    /**
     * Delete tool
     */
    @DELETE
    @Path("/{toolId}")
    public Response deleteTool(@PathParam("toolId") String toolId) {
        try {
            boolean deleted = registry.deleteTool(toolId);
            if (deleted) {
                return Response.ok(successResponse("Tool deleted successfully")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse("Tool not found: " + toolId)).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse("Failed to delete tool: " + e.getMessage())).build();
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
