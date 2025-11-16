package com.example.agent.registry;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A router agent that dynamically discovers and delegates to agents from the registry.
 * This allows for dynamic agent discovery and routing without hardcoding agent references.
 */
public class RegistryAwareRouter {
    
    private static final Logger LOGGER = Logger.getLogger(RegistryAwareRouter.class.getName());
    
    private final AgentRegistry registry;
    private final DynamicAgentLoader loader;
    private final BaseAgent routerAgent;
    private final InMemoryRunner runner;
    
    public RegistryAwareRouter(AgentRegistry registry, DynamicAgentLoader loader) {
        this.registry = registry;
        this.loader = loader;
        this.routerAgent = createRouterAgent();
        this.runner = new InMemoryRunner(routerAgent);
    }
    
    /**
     * Create the main router agent that delegates to registered agents
     */
    private BaseAgent createRouterAgent() {
        return LlmAgent.builder()
            .name("registry-router")
            .description("Routes requests to dynamically registered agents")
            .instruction(buildRouterInstruction())
            .model("gemini-2.5-flash")
            .build();
    }
    
    /**
     * Build dynamic routing instructions based on registered agents
     */
    private String buildRouterInstruction() {
        StringBuilder instruction = new StringBuilder();
        instruction.append("You are an intelligent router that delegates tasks to specialized agents.\n\n");
        instruction.append("Available agents:\n");
        
        try {
            List<AgentMetadata> agents = registry.getAllAgents();
            for (AgentMetadata agent : agents) {
                instruction.append(String.format("- %s: %s\n", 
                    agent.getName(), 
                    agent.getDescription()));
                
                if (agent.getCapabilities() != null && !agent.getCapabilities().isEmpty()) {
                    instruction.append("  Capabilities: ");
                    instruction.append(String.join(", ", agent.getCapabilities()));
                    instruction.append("\n");
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load agent metadata for routing: " + e.getMessage());
        }
        
        instruction.append("\nAnalyze the user's request and route it to the most appropriate agent.");
        return instruction.toString();
    }
    
    /**
     * Route a user request to the appropriate agent
     */
    public Flowable<Event> route(String userId, String sessionId, Content userMessage, RunConfig runConfig) {
        try {
            // Analyze user request to determine target agent
            String targetAgentId = determineTargetAgent(userMessage);
            
            if (targetAgentId == null) {
                LOGGER.warning("No suitable agent found for request");
                return Flowable.error(new RuntimeException("No suitable agent found"));
            }
            
            // Load the target agent if not already loaded
            BaseAgent targetAgent = loader.getLoadedAgent(targetAgentId);
            if (targetAgent == null) {
                targetAgent = loader.loadAgent(targetAgentId);
            }
            
            // Create a runner for the target agent and execute
            InMemoryRunner targetRunner = new InMemoryRunner(targetAgent);
            return targetRunner.runAsync(userId, sessionId, userMessage, runConfig);
            
        } catch (Exception e) {
            LOGGER.severe("Routing error: " + e.getMessage());
            return Flowable.error(e);
        }
    }
    
    /**
     * Determine which agent should handle the request
     * This uses a simple keyword matching approach, but could be enhanced with LLM-based routing
     */
    private String determineTargetAgent(Content userMessage) throws Exception {
        // Extract text from Content using the built-in text() method
        String messageText = userMessage.text();
        if (messageText == null) {
            messageText = "";
        }
        messageText = messageText.toLowerCase();
        
        List<AgentMetadata> allAgents = registry.getAllAgents();
        
        // Try to match based on capabilities
        for (AgentMetadata agent : allAgents) {
            if (agent.getCapabilities() != null) {
                for (String capability : agent.getCapabilities()) {
                    if (messageText.contains(capability.toLowerCase())) {
                        LOGGER.info("Routing to agent: " + agent.getName() + " based on capability: " + capability);
                        return agent.getAgentId();
                    }
                }
            }
        }
        
        // Try to match based on agent name/description keywords
        for (AgentMetadata agent : allAgents) {
            String[] keywords = agent.getName().toLowerCase().split("[\\s-_]+");
            for (String keyword : keywords) {
                if (messageText.contains(keyword)) {
                    LOGGER.info("Routing to agent: " + agent.getName() + " based on keyword: " + keyword);
                    return agent.getAgentId();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get the router's InMemoryRunner for session management
     */
    public InMemoryRunner getRunner() {
        return runner;
    }
    
    /**
     * Refresh router instructions when agents are updated
     */
    public void refreshRouter() {
        LOGGER.info("Refreshing router with updated agent information");
        // Note: In practice, you might want to rebuild the router agent
        // or use a more dynamic instruction updating mechanism
    }
    
    /**
     * List all available agents
     */
    public List<AgentInfo> listAvailableAgents() throws Exception {
        List<AgentInfo> agentInfos = new ArrayList<>();
        List<AgentMetadata> agents = registry.getAllAgents();
        
        for (AgentMetadata agent : agents) {
            boolean loaded = loader.isAgentLoaded(agent.getAgentId());
            agentInfos.add(new AgentInfo(
                agent.getAgentId(),
                agent.getName(),
                agent.getDescription(),
                agent.getCapabilities(),
                agent.getStatus(),
                loaded
            ));
        }
        
        return agentInfos;
    }
    
    /**
     * Information about an available agent
     */
    public static class AgentInfo {
        public final String agentId;
        public final String name;
        public final String description;
        public final List<String> capabilities;
        public final AgentMetadata.AgentStatus status;
        public final boolean loaded;
        
        public AgentInfo(String agentId, String name, String description, 
                        List<String> capabilities, AgentMetadata.AgentStatus status, 
                        boolean loaded) {
            this.agentId = agentId;
            this.name = name;
            this.description = description;
            this.capabilities = capabilities;
            this.status = status;
            this.loaded = loaded;
        }
    }
}
