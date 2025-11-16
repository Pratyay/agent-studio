package com.example.agent.registry;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * REST API for generating agent code
 */
@Path("/api/agents/generate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentGeneratorResource {
    
    private final AgentCodeGenerator generator = new AgentCodeGenerator();
    private final ToolRegistry toolRegistry;
    
    public AgentGeneratorResource(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }
    
    public static class GenerateRequest {
        public String agentName;
        public String description;
        public String packageName;
        public List<String> toolIds;
        
        public GenerateRequest() {
            this.toolIds = new ArrayList<>();
            this.packageName = "com.example.agent";
        }
    }
    
    /**
     * Generate agent code and return as downloadable ZIP
     */
    @POST
    @Produces("application/zip")
    public Response generateAgent(GenerateRequest request) {
        try {
            // Validate input
            if (request.agentName == null || request.agentName.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", "Agent name is required"))
                    .build();
            }
            
            if (request.toolIds == null || request.toolIds.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", "At least one tool must be attached"))
                    .build();
            }
            
            // Fetch tool metadata
            List<ToolMetadata> tools = new ArrayList<>();
            for (String toolId : request.toolIds) {
                Optional<ToolMetadata> tool = toolRegistry.getTool(toolId);
                if (tool.isPresent()) {
                    tools.add(tool.get());
                } else {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("error", "Tool not found: " + toolId))
                        .build();
                }
            }
            
            // Create generation request
            AgentCodeGenerator.GenerationRequest genRequest = new AgentCodeGenerator.GenerationRequest();
            genRequest.agentName = request.agentName;
            genRequest.description = request.description;
            genRequest.packageName = request.packageName != null ? request.packageName : "com.example.agent";
            genRequest.attachedTools = tools;
            
            // Generate ZIP
            byte[] zipBytes = generator.generateAgentZip(genRequest);
            
            // Return ZIP as download
            String filename = request.agentName.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-agent.zip";
            return Response.ok(zipBytes)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "application/zip")
                .build();
                
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", "Failed to generate agent: " + e.getMessage()))
                .build();
        }
    }
}
