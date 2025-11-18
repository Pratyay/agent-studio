package com.example.agent.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.logging.Logger;

/**
 * Registry for managing available agent callbacks.
 */
public class CallbackRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(CallbackRegistry.class.getName());
    private static final String CALLBACK_KEY_PREFIX = "callback:";
    private static final String CALLBACK_LIST_KEY = "callbacks:list";
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    
    public CallbackRegistry() {
        this("localhost", 6379);
    }
    
    public CallbackRegistry(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        
        this.jedisPool = new JedisPool(poolConfig, host, port);
        this.objectMapper = new ObjectMapper();
        
        // Initialize default callbacks on startup
        try {
            initializeDefaultCallbacks();
        } catch (Exception e) {
            LOGGER.severe("Failed to initialize default callbacks: " + e.getMessage());
        }
    }
    
    /**
     * Initialize default callbacks from agent-studio-callbacks library
     */
    private void initializeDefaultCallbacks() throws Exception {
        List<CallbackMetadata> defaultCallbacks = Arrays.asList(
            new CallbackMetadata(
                "logging-callback",
                "Logging Callback",
                "Logs all incoming messages with timestamp, session ID, agent name, and invocation ID. Useful for debugging and audit trails.",
                "com.example.agent.callbacks.LoggingCallback",
                "BEFORE_AGENT",
                Arrays.asList("logging", "audit", "debugging")
            ),
            new CallbackMetadata(
                "metrics-callback",
                "Metrics Callback",
                "Collects usage metrics including total message count and per-session message counts. Tracks agent activity across sessions.",
                "com.example.agent.callbacks.MetricsCallback",
                "BEFORE_AGENT",
                Arrays.asList("metrics", "analytics", "monitoring")
            ),
            new CallbackMetadata(
                "security-callback",
                "Security Callback",
                "Performs basic security checks on incoming messages. Blocks messages containing script tags, JavaScript protocols, eval() calls, and event handlers.",
                "com.example.agent.callbacks.SecurityCallback",
                "BEFORE_AGENT",
                Arrays.asList("security", "validation", "filtering")
            ),
            new CallbackMetadata(
                "rate-limit-callback",
                "Rate Limit Callback",
                "Enforces rate limiting on a per-session basis. Default limit is 10 messages per minute. Returns error message if limit exceeded.",
                "com.example.agent.callbacks.RateLimitCallback",
                "BEFORE_AGENT",
                Arrays.asList("rate-limiting", "throttling", "protection")
            )
        );
        
        for (CallbackMetadata callback : defaultCallbacks) {
            // Only register if not already present
            if (getCallback(callback.getCallbackId()).isEmpty()) {
                registerCallback(callback);
            }
        }
        
        LOGGER.info("Initialized " + defaultCallbacks.size() + " default callbacks");
    }
    
    /**
     * Register a callback
     */
    public CallbackMetadata registerCallback(CallbackMetadata metadata) throws Exception {
        if (metadata.getCallbackId() == null) {
            metadata.setCallbackId(UUID.randomUUID().toString());
        }
        
        long now = System.currentTimeMillis();
        metadata.setCreatedAt(now);
        metadata.setUpdatedAt(now);
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = CALLBACK_KEY_PREFIX + metadata.getCallbackId();
            String json = objectMapper.writeValueAsString(metadata);
            
            jedis.set(key, json);
            jedis.sadd(CALLBACK_LIST_KEY, metadata.getCallbackId());
            
            LOGGER.info("Registered callback: " + metadata.getName() + " (ID: " + metadata.getCallbackId() + ")");
            return metadata;
        }
    }
    
    /**
     * Get callback by ID
     */
    public Optional<CallbackMetadata> getCallback(String callbackId) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = CALLBACK_KEY_PREFIX + callbackId;
            String json = jedis.get(key);
            
            if (json == null) {
                return Optional.empty();
            }
            
            CallbackMetadata metadata = objectMapper.readValue(json, CallbackMetadata.class);
            return Optional.of(metadata);
        }
    }
    
    /**
     * Get all callbacks
     */
    public List<CallbackMetadata> getAllCallbacks() throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> callbackIds = jedis.smembers(CALLBACK_LIST_KEY);
            List<CallbackMetadata> callbacks = new ArrayList<>();
            
            for (String callbackId : callbackIds) {
                Optional<CallbackMetadata> callback = getCallback(callbackId);
                callback.ifPresent(callbacks::add);
            }
            
            // Sort by name
            callbacks.sort(Comparator.comparing(CallbackMetadata::getName));
            
            return callbacks;
        }
    }
    
    /**
     * Get callbacks by type (BEFORE_AGENT or AFTER_AGENT)
     */
    public List<CallbackMetadata> getCallbacksByType(String type) throws Exception {
        List<CallbackMetadata> allCallbacks = getAllCallbacks();
        List<CallbackMetadata> filtered = new ArrayList<>();
        
        for (CallbackMetadata callback : allCallbacks) {
            if (type.equalsIgnoreCase(callback.getType())) {
                filtered.add(callback);
            }
        }
        
        return filtered;
    }
    
    /**
     * Update callback
     */
    public CallbackMetadata updateCallback(String callbackId, CallbackMetadata metadata) throws Exception {
        Optional<CallbackMetadata> existing = getCallback(callbackId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Callback not found: " + callbackId);
        }
        
        metadata.setCallbackId(callbackId);
        metadata.setCreatedAt(existing.get().getCreatedAt());
        metadata.setUpdatedAt(System.currentTimeMillis());
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = CALLBACK_KEY_PREFIX + callbackId;
            String json = objectMapper.writeValueAsString(metadata);
            jedis.set(key, json);
            
            LOGGER.info("Updated callback: " + callbackId);
            return metadata;
        }
    }
    
    /**
     * Delete callback
     */
    public boolean deleteCallback(String callbackId) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = CALLBACK_KEY_PREFIX + callbackId;
            Long deleted = jedis.del(key);
            jedis.srem(CALLBACK_LIST_KEY, callbackId);
            
            if (deleted > 0) {
                LOGGER.info("Deleted callback: " + callbackId);
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * Close registry
     */
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
