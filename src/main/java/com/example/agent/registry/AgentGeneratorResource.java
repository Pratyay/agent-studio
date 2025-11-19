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
    private final A2AClientService a2aClientService;
    
    public AgentGeneratorResource(ToolRegistry toolRegistry, A2AClientService a2aClientService) {
        this.toolRegistry = toolRegistry;
        this.a2aClientService = a2aClientService;
    }
    
    public static class CustomCallbackDefinition {
        public String name;
        public String description;
        public String type; // "before" or "after" - for organization in UI
        public String category; // "BeforeAgentCallback", "AfterAgentCallback", etc.
        public String state; // "stateless" or "stateful"
        public String lambda; // Lambda code if stateless
        public String scope; // "ApplicationScoped", "RequestScoped", or "SessionScoped"
        public Boolean needsProducer; // true if a CDI Producer class is needed
    }
    
    public static class GenerateRequest {
        public String agentName;
        public String description;
        public String instructions;
        public String packageName;
        public List<String> toolIds;
        public List<String> subagentIds;
        public String googleApiKey;
        public Integer serverPort;
        public List<String> beforeAgentCallbacks;
        public List<String> afterAgentCallbacks;
        public List<String> transportProtocols;
        public List<CustomCallbackDefinition> customCallbacks;
        
        public GenerateRequest() {
            this.toolIds = new ArrayList<>();
            this.subagentIds = new ArrayList<>();
            this.packageName = "com.example.agent";
            this.beforeAgentCallbacks = new ArrayList<>();
            this.afterAgentCallbacks = new ArrayList<>();
            this.transportProtocols = new ArrayList<>();
            this.customCallbacks = new ArrayList<>();
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
            
            // Tools and sub-agents are optional - can have one, both, or neither
            
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
            
            // Fetch subagent metadata
            List<A2AAgentMetadata> subagents = new ArrayList<>();
            System.out.println("[AgentGenerator] Received subagentIds: " + request.subagentIds);
            if (request.subagentIds != null && !request.subagentIds.isEmpty()) {
                System.out.println("[AgentGenerator] Processing " + request.subagentIds.size() + " subagent IDs");
                for (String subagentId : request.subagentIds) {
                    System.out.println("[AgentGenerator] Fetching subagent: " + subagentId);
                    A2AAgentMetadata subagent = a2aClientService.getAgent(subagentId);
                    if (subagent != null) {
                        System.out.println("[AgentGenerator] Found subagent: " + subagent.getName() + " at " + subagent.getUrl());
                        subagents.add(subagent);
                    } else {
                        System.out.println("[AgentGenerator] Subagent not found: " + subagentId);
                        return Response.status(Response.Status.BAD_REQUEST)
                            .type(MediaType.APPLICATION_JSON)
                            .entity(Map.of("error", "Sub-agent not found: " + subagentId))
                            .build();
                    }
                }
            }
            System.out.println("[AgentGenerator] Total subagents to include: " + subagents.size());
            
            // Create generation request
            AgentCodeGenerator.GenerationRequest genRequest = new AgentCodeGenerator.GenerationRequest();
            genRequest.agentName = request.agentName;
            genRequest.description = request.description;
            genRequest.instructions = request.instructions;
            genRequest.packageName = request.packageName != null ? request.packageName : "com.example.agent";
            genRequest.attachedTools = tools;
            genRequest.subagents = subagents;
            genRequest.googleApiKey = request.googleApiKey;
            genRequest.serverPort = request.serverPort;
            genRequest.beforeAgentCallbacks = request.beforeAgentCallbacks != null ? request.beforeAgentCallbacks : new ArrayList<>();
            genRequest.afterAgentCallbacks = request.afterAgentCallbacks != null ? request.afterAgentCallbacks : new ArrayList<>();
            genRequest.transportProtocols = request.transportProtocols != null ? request.transportProtocols : new ArrayList<>();
            genRequest.customCallbacks = request.customCallbacks != null ? request.customCallbacks : new ArrayList<>();
            
            System.out.println("[AgentGenerator] Creating agent with:");
            System.out.println("  - MCP Tools: " + tools.size());
            System.out.println("  - Sub-agents: " + subagents.size());
            System.out.println("  - Transport Protocols: " + genRequest.transportProtocols);
            System.out.println("  - Custom Callbacks: " + request.customCallbacks.size());
            for (CustomCallbackDefinition cb : request.customCallbacks) {
                System.out.println("    [RECEIVED] " + cb.name + " - type: " + cb.type + ", category: " + cb.category + ", state: " + cb.state);
            }
            
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
