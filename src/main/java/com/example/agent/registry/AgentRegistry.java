package com.example.agent.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry for managing agent metadata with Redis backend.
 * Provides CRUD operations and pub/sub notifications for agent changes.
 */
public class AgentRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(AgentRegistry.class.getName());
    private static final String AGENT_KEY_PREFIX = "agent:";
    private static final String AGENT_LIST_KEY = "agents:list";
    private static final String AGENT_UPDATES_CHANNEL = "agent:updates";
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final Map<String, List<AgentChangeListener>> listeners;
    
    public AgentRegistry() {
        this("localhost", 6379);
    }
    
    public AgentRegistry(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        
        this.jedisPool = new JedisPool(poolConfig, host, port);
        this.objectMapper = new ObjectMapper();
        this.listeners = new ConcurrentHashMap<>();
        
        // Start pub/sub listener in background thread
        startPubSubListener();
    }
    
    /**
     * Register a new agent in the registry
     */
    public AgentMetadata registerAgent(AgentMetadata metadata) throws Exception {
        if (metadata.getAgentId() == null) {
            metadata.setAgentId(UUID.randomUUID().toString());
        }
        
        long now = System.currentTimeMillis();
        metadata.setCreatedAt(now);
        metadata.setUpdatedAt(now);
        
        if (metadata.getStatus() == null) {
            metadata.setStatus(AgentMetadata.AgentStatus.ACTIVE);
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = AGENT_KEY_PREFIX + metadata.getAgentId();
            String json = objectMapper.writeValueAsString(metadata);
            
            jedis.set(key, json);
            jedis.sadd(AGENT_LIST_KEY, metadata.getAgentId());
            
            // Publish agent registration event
            publishAgentChange("REGISTERED", metadata.getAgentId());
            
            LOGGER.info("Registered agent: " + metadata.getName() + " (ID: " + metadata.getAgentId() + ")");
            return metadata;
        }
    }
    
    /**
     * Get agent metadata by ID
     */
    public Optional<AgentMetadata> getAgent(String agentId) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = AGENT_KEY_PREFIX + agentId;
            String json = jedis.get(key);
            
            if (json == null) {
                return Optional.empty();
            }
            
            AgentMetadata metadata = objectMapper.readValue(json, AgentMetadata.class);
            return Optional.of(metadata);
        }
    }
    
    /**
     * Get all registered agents
     */
    public List<AgentMetadata> getAllAgents() throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> agentIds = jedis.smembers(AGENT_LIST_KEY);
            List<AgentMetadata> agents = new ArrayList<>();
            
            for (String agentId : agentIds) {
                Optional<AgentMetadata> agent = getAgent(agentId);
                agent.ifPresent(agents::add);
            }
            
            return agents;
        }
    }
    
    /**
     * Find agents by capability
     */
    public List<AgentMetadata> findAgentsByCapability(String capability) throws Exception {
        List<AgentMetadata> allAgents = getAllAgents();
        List<AgentMetadata> matchingAgents = new ArrayList<>();
        
        for (AgentMetadata agent : allAgents) {
            if (agent.getCapabilities() != null && 
                agent.getCapabilities().contains(capability)) {
                matchingAgents.add(agent);
            }
        }
        
        return matchingAgents;
    }
    
    /**
     * Update agent metadata
     */
    public AgentMetadata updateAgent(String agentId, AgentMetadata metadata) throws Exception {
        Optional<AgentMetadata> existing = getAgent(agentId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }
        
        metadata.setAgentId(agentId);
        metadata.setCreatedAt(existing.get().getCreatedAt());
        metadata.setUpdatedAt(System.currentTimeMillis());
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = AGENT_KEY_PREFIX + agentId;
            String json = objectMapper.writeValueAsString(metadata);
            jedis.set(key, json);
            
            publishAgentChange("UPDATED", agentId);
            
            LOGGER.info("Updated agent: " + agentId);
            return metadata;
        }
    }
    
    /**
     * Unregister an agent
     */
    public boolean unregisterAgent(String agentId) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = AGENT_KEY_PREFIX + agentId;
            Long deleted = jedis.del(key);
            jedis.srem(AGENT_LIST_KEY, agentId);
            
            if (deleted > 0) {
                publishAgentChange("UNREGISTERED", agentId);
                LOGGER.info("Unregistered agent: " + agentId);
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * Publish agent change event
     */
    private void publishAgentChange(String eventType, String agentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String message = eventType + ":" + agentId;
            jedis.publish(AGENT_UPDATES_CHANNEL, message);
        }
    }
    
    /**
     * Start pub/sub listener for agent changes
     */
    private void startPubSubListener() {
        Thread listenerThread = new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        String[] parts = message.split(":", 2);
                        if (parts.length == 2) {
                            String eventType = parts[0];
                            String agentId = parts[1];
                            notifyListeners(eventType, agentId);
                        }
                    }
                }, AGENT_UPDATES_CHANNEL);
            } catch (Exception e) {
                LOGGER.severe("PubSub listener error: " + e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    /**
     * Add a listener for agent changes
     */
    public void addChangeListener(String eventType, AgentChangeListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }
    
    /**
     * Notify listeners of agent changes
     */
    private void notifyListeners(String eventType, String agentId) {
        List<AgentChangeListener> typeListeners = listeners.get(eventType);
        if (typeListeners != null) {
            for (AgentChangeListener listener : typeListeners) {
                try {
                    listener.onAgentChange(eventType, agentId);
                } catch (Exception e) {
                    LOGGER.warning("Listener error: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Close the registry and release resources
     */
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
    
    /**
     * Interface for listening to agent changes
     */
    @FunctionalInterface
    public interface AgentChangeListener {
        void onAgentChange(String eventType, String agentId) throws Exception;
    }
}
