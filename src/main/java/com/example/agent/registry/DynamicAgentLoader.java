package com.example.agent.registry;

import com.google.adk.agents.BaseAgent;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Dynamically loads agent JARs at runtime using custom class loaders.
 * Supports hot-loading and unloading of agents without restarting the application.
 */
public class DynamicAgentLoader {
    
    private static final Logger LOGGER = Logger.getLogger(DynamicAgentLoader.class.getName());
    
    private final Map<String, LoadedAgent> loadedAgents;
    private final AgentRegistry registry;
    
    public DynamicAgentLoader(AgentRegistry registry) {
        this.loadedAgents = new ConcurrentHashMap<>();
        this.registry = registry;
        
        // Listen for agent registration events
        registry.addChangeListener("REGISTERED", this::onAgentRegistered);
        registry.addChangeListener("UNREGISTERED", this::onAgentUnregistered);
    }
    
    /**
     * Load an agent from its JAR file
     */
    public BaseAgent loadAgent(String agentId) throws Exception {
        // Check if already loaded
        if (loadedAgents.containsKey(agentId)) {
            return loadedAgents.get(agentId).agent;
        }
        
        // Get metadata from registry
        AgentMetadata metadata = registry.getAgent(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
        
        File jarFile = new File(metadata.getJarPath());
        if (!jarFile.exists()) {
            throw new IllegalArgumentException("Agent JAR not found: " + metadata.getJarPath());
        }
        
        // Create custom class loader for the agent
        URL[] urls = {jarFile.toURI().toURL()};
        URLClassLoader classLoader = new URLClassLoader(
            urls,
            this.getClass().getClassLoader()
        );
        
        // Load the agent class
        Class<?> agentClass = classLoader.loadClass(metadata.getMainClass());
        
        // Get the ROOT_AGENT static field (following ADK pattern)
        Field rootAgentField = agentClass.getDeclaredField("ROOT_AGENT");
        rootAgentField.setAccessible(true);
        BaseAgent agent = (BaseAgent) rootAgentField.get(null);
        
        // Cache the loaded agent
        LoadedAgent loadedAgent = new LoadedAgent(agent, classLoader, metadata);
        loadedAgents.put(agentId, loadedAgent);
        
        LOGGER.info("Loaded agent: " + metadata.getName() + " (ID: " + agentId + ")");
        return agent;
    }
    
    /**
     * Unload an agent and release its resources
     */
    public boolean unloadAgent(String agentId) throws Exception {
        LoadedAgent loadedAgent = loadedAgents.remove(agentId);
        if (loadedAgent != null) {
            // Close the class loader to release resources
            loadedAgent.classLoader.close();
            LOGGER.info("Unloaded agent: " + agentId);
            return true;
        }
        return false;
    }
    
    /**
     * Get a loaded agent by ID
     */
    public BaseAgent getLoadedAgent(String agentId) {
        LoadedAgent loadedAgent = loadedAgents.get(agentId);
        return loadedAgent != null ? loadedAgent.agent : null;
    }
    
    /**
     * Get metadata for a loaded agent
     */
    public AgentMetadata getLoadedAgentMetadata(String agentId) {
        LoadedAgent loadedAgent = loadedAgents.get(agentId);
        return loadedAgent != null ? loadedAgent.metadata : null;
    }
    
    /**
     * Check if an agent is loaded
     */
    public boolean isAgentLoaded(String agentId) {
        return loadedAgents.containsKey(agentId);
    }
    
    /**
     * Get all loaded agent IDs
     */
    public Map<String, LoadedAgent> getAllLoadedAgents() {
        return new ConcurrentHashMap<>(loadedAgents);
    }
    
    /**
     * Handle agent registration event
     */
    private void onAgentRegistered(String eventType, String agentId) throws Exception {
        LOGGER.info("Agent registered, attempting to load: " + agentId);
        try {
            loadAgent(agentId);
        } catch (Exception e) {
            LOGGER.warning("Failed to auto-load agent " + agentId + ": " + e.getMessage());
            
            // Update agent status to ERROR
            AgentMetadata metadata = registry.getAgent(agentId).orElse(null);
            if (metadata != null) {
                metadata.setStatus(AgentMetadata.AgentStatus.ERROR);
                registry.updateAgent(agentId, metadata);
            }
        }
    }
    
    /**
     * Handle agent unregistration event
     */
    private void onAgentUnregistered(String eventType, String agentId) throws Exception {
        LOGGER.info("Agent unregistered, unloading: " + agentId);
        unloadAgent(agentId);
    }
    
    /**
     * Container for a loaded agent and its metadata
     */
    public static class LoadedAgent {
        public final BaseAgent agent;
        public final URLClassLoader classLoader;
        public final AgentMetadata metadata;
        
        public LoadedAgent(BaseAgent agent, URLClassLoader classLoader, AgentMetadata metadata) {
            this.agent = agent;
            this.classLoader = classLoader;
            this.metadata = metadata;
        }
    }
}
