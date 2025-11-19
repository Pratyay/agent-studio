package com.example.agent.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.transport.grpc.GrpcTransport;
import io.a2a.client.transport.grpc.GrpcTransportConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.client.transport.rest.RestTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import io.a2a.spec.Task;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Service for managing A2A client connections
 */
public class A2AClientService {

    private static final Logger logger = LoggerFactory.getLogger(A2AClientService.class);
    private static final String REDIS_KEY_PREFIX = "a2a:agent:";

    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();
    private final Map<String, String> agentUrls = new ConcurrentHashMap<>();
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public A2AClientService(String redisHost, int redisPort) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        this.jedisPool = new JedisPool(poolConfig, redisHost, redisPort);

        // Load existing agents from Redis on startup
        loadAgentsFromRedis();
    }

    /**
     * Connect to an A2A agent and retrieve its agent card
     */
    public A2AAgentMetadata connectToAgent(String agentUrl) throws Exception {
        logger.info("Connecting to A2A agent at: {}", agentUrl);

        try {
            // Fetch the agent card using A2ACardResolver
            AgentCard agentCard = fetchAgentCard(agentUrl);

            if (agentCard == null) {
                throw new Exception("Failed to retrieve agent card from: " + agentUrl);
            }

            // Generate agent ID
            String agentId = generateAgentId(agentCard.name(), agentUrl);

            // Create A2A client with proper transport configuration
            Client client = createClient(agentCard, agentId);
            clients.put(agentId, client);
            agentUrls.put(agentId, agentUrl);

            // Convert agent card to metadata
            A2AAgentMetadata metadata = convertAgentCardToMetadata(agentId, agentCard, agentUrl);

            // Save to Redis
            saveAgentToRedis(metadata);

            logger.info("Successfully connected to A2A agent: {} ({})", agentCard.name(), agentId);

            return metadata;

        } catch (Exception e) {
            logger.error("Failed to connect to A2A agent at {}: {}", agentUrl, e.getMessage(), e);
            throw new Exception("Failed to connect to A2A agent: " + e.getMessage(), e);
        }
    }

    /**
     * Create an A2A client with proper transport and event handlers
     */
    private Client createClient(AgentCard agentCard, String agentId) {

        // Create client config to accept streaming
        ClientConfig clientConfig = new ClientConfig.Builder()
                .setAcceptedOutputModes(List.of("text"))
                .build();

        // Create consumers list for handling client events
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();
        consumers.add((event, card) -> {
            String eventType = event.getClass().getSimpleName();
            logger.info("[A2A Event for {}] Received: {} ({})", agentId, eventType, event.getClass().getName());

            StringBuilder textBuilder = new StringBuilder();

            // Handle MessageEvent
            if (event instanceof MessageEvent messageEvent) {
                Message responseMessage = messageEvent.getMessage();
                if (responseMessage.getParts() != null) {
                    for (Part<?> part : responseMessage.getParts()) {
                        if (part instanceof TextPart textPart) {
                            textBuilder.append(textPart.getText());
                        }
                    }
                }
            }

            // Try to extract task and artifacts from TaskUpdateEvent
            try {
                var getTask = event.getClass().getMethod("getTask");
                Task task = (Task) getTask.invoke(event);
                if (task != null && task.getArtifacts() != null) {
                    logger.info("Task has {} artifacts", task.getArtifacts().size());
                    for (var artifact : task.getArtifacts()) {
                        if (artifact.parts() != null) {
                            for (Part<?> part : artifact.parts()) {
                                if (part instanceof TextPart textPart) {
                                    textBuilder.append(textPart.getText());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not get task from event: {}", e.getMessage());
            }

            // Also try getArtifact().getParts() for direct artifact events
            try {
                var getArtifact = event.getClass().getMethod("getArtifact");
                Object artifact = getArtifact.invoke(event);
                if (artifact != null) {
                    var getParts = artifact.getClass().getMethod("getParts");
                    @SuppressWarnings("unchecked")
                    List<Part<?>> parts = (List<Part<?>>) getParts.invoke(artifact);
                    if (parts != null) {
                        for (Part<?> part : parts) {
                            if (part instanceof TextPart textPart) {
                                textBuilder.append(textPart.getText());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Not an artifact event
            }

            if (textBuilder.length() > 0) {
                String responseText = textBuilder.toString();
                logger.info("Extracted text response from {}: {}", agentId, responseText);

                // Complete any pending future for this agent
                CompletableFuture<String> future = pendingResponses.remove(agentId);
                if (future != null) {
                    future.complete(responseText);
                }
            }
        });

        // Create error handler for streaming errors
        Consumer<Throwable> streamingErrorHandler = (error) -> {
            logger.error("Streaming error occurred: {}", error.getMessage(), error);
        };


        // Build client with JSONRPC transport
        Client client = Client
                .builder(agentCard)
                .clientConfig(clientConfig)
                .addConsumers(consumers)
                .streamingErrorHandler(streamingErrorHandler)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                .withTransport(RestTransport.class, new RestTransportConfig())
                .build();
        
        logger.info("Created A2A client for agent: {}", agentId);
        return client;
    }
    
    /**
     * Get all registered A2A agents
     */
    public List<A2AAgentMetadata> getAllAgents() {
        List<A2AAgentMetadata> agents = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys(REDIS_KEY_PREFIX + "*");
            for (String key : keys) {
                String json = jedis.get(key);
                if (json != null) {
                    A2AAgentMetadata metadata = objectMapper.readValue(json, A2AAgentMetadata.class);
                    agents.add(metadata);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load agents from Redis", e);
        }
        return agents;
    }
    
    /**
     * Get a specific A2A agent by ID
     */
    public A2AAgentMetadata getAgent(String agentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(REDIS_KEY_PREFIX + agentId);
            if (json != null) {
                return objectMapper.readValue(json, A2AAgentMetadata.class);
            }
        } catch (Exception e) {
            logger.error("Failed to load agent {} from Redis", agentId, e);
        }
        return null;
    }
    
    /**
     * Get A2A client for an agent
     */
    public Client getClient(String agentId) {
        return clients.get(agentId);
    }
    
    /**
     * Send a message to an A2A agent and wait for response
     */
    public String sendMessage(String agentId, String messageText) throws Exception {
        return sendMessage(agentId, messageText, 60); // 60 second timeout
    }
    
    /**
     * Send a message to an A2A agent and wait for response with timeout
     */
    public String sendMessage(String agentId, String messageText, int timeoutSeconds) throws Exception {
        Client client = clients.get(agentId);
        if (client == null) {
            throw new Exception("No client found for agent ID: " + agentId);
        }
        
        try {
            // Create a future to capture the response
            CompletableFuture<String> responseFuture = new CompletableFuture<>();
            pendingResponses.put(agentId, responseFuture);
            
            // Create a text part and message
            TextPart textPart = new TextPart(messageText, null);
            List<Part<?>> parts = new ArrayList<>();
            parts.add(textPart);
            Message message = new Message(
                Message.Role.USER, 
                parts, 
                java.util.UUID.randomUUID().toString(), 
                null, 
                null, 
                null, 
                null
            );
            
            logger.info("Sending message to agent {}: {}", agentId, messageText);
            logger.info("Client methods available: {}", java.util.Arrays.toString(client.getClass().getMethods()));
            
            // Try to start/connect first
            try {
                var startMethod = client.getClass().getMethod("start");
                logger.info("Calling start() on client");
                startMethod.invoke(client);
            } catch (NoSuchMethodException e) {
                logger.debug("No start() method found");
            }
            
            client.sendMessage(message);
            logger.info("Message sent successfully via A2A client");
            
            // Wait for response with timeout
            try {
                String response = responseFuture.get(timeoutSeconds, TimeUnit.SECONDS);
                logger.info("Received response from agent {}: {}", agentId, response);
                return response;
            } catch (TimeoutException e) {
                pendingResponses.remove(agentId);
                throw new Exception("Timeout waiting for response from agent (" + timeoutSeconds + "s)");
            }
            
        } catch (Exception e) {
            logger.error("Failed to send message to agent {}: {}", agentId, e.getMessage(), e);
            throw new Exception("Failed to send message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Disconnect from an A2A agent
     */
    public boolean disconnectAgent(String agentId) {
        logger.info("Disconnecting from A2A agent: {}", agentId);
        
        Client client = clients.remove(agentId);
        
        // Remove from Redis
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(REDIS_KEY_PREFIX + agentId);
            logger.info("Successfully disconnected from A2A agent: {}", agentId);
            return true;
        } catch (Exception e) {
            logger.error("Error disconnecting from agent {}: {}", agentId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Test connection to an A2A agent
     */
    public Map<String, Object> testConnection(String agentUrl) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            AgentCard agentCard = fetchAgentCard(agentUrl);
            
            if (agentCard != null) {
                result.put("success", true);
                result.put("name", agentCard.name());
                result.put("description", agentCard.description());
                result.put("version", agentCard.version());
                result.put("protocolVersion", agentCard.protocolVersion());
            } else {
                result.put("success", false);
                result.put("error", "Failed to retrieve agent card");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Fetch agent card from A2A server using A2ACardResolver
     */
    private AgentCard fetchAgentCard(String agentUrl) throws Exception {
        try {
            // A2ACardResolver automatically tries .well-known/agent-card.json
            // Just pass the base URL
            A2ACardResolver resolver = new A2ACardResolver(agentUrl);
            AgentCard agentCard = resolver.getAgentCard();
            logger.info("Successfully fetched agent card from: {}", agentUrl);
            logger.debug("Agent card: {}", objectMapper.writeValueAsString(agentCard));
            
            return agentCard;
            
        } catch (Exception e) {
            logger.error("Failed to fetch agent card from {}: {}", agentUrl, e.getMessage());
            throw new Exception("Failed to fetch agent card: " + e.getMessage(), e);
        }
    }
    
    private A2AAgentMetadata convertAgentCardToMetadata(String agentId, AgentCard card, String url) {
        A2AAgentMetadata metadata = new A2AAgentMetadata();
        metadata.setId(agentId);
        metadata.setName(card.name());
        metadata.setDescription(card.description());
        metadata.setUrl(url);
        metadata.setVersion(card.version());
        metadata.setProtocolVersion(card.protocolVersion());
        
        // Convert capabilities
        if (card.capabilities() != null) {
            Map<String, Object> caps = new HashMap<>();
            caps.put("streaming", card.capabilities().streaming());
            caps.put("pushNotifications", card.capabilities().pushNotifications());
            caps.put("stateTransitionHistory", card.capabilities().stateTransitionHistory());
            metadata.setCapabilities(caps);
        }
        
        // Convert skills
        if (card.skills() != null) {
            List<Map<String, Object>> skills = new ArrayList<>();
            card.skills().forEach(skill -> {
                Map<String, Object> skillMap = new HashMap<>();
                skillMap.put("id", skill.id());
                skillMap.put("name", skill.name());
                skillMap.put("description", skill.description());
                skillMap.put("tags", skill.tags());
                skillMap.put("examples", skill.examples());
                skills.add(skillMap);
            });
            metadata.setSkills(skills);
        }
        
        metadata.setInputModes(card.defaultInputModes());
        metadata.setOutputModes(card.defaultOutputModes());
        metadata.setStatus("connected");
        metadata.setLastUpdated(System.currentTimeMillis());
        
        // Extract transport protocol information using reflection/JSON
        try {
            // Convert AgentCard to JSON and back to extract all fields
            String cardJson = objectMapper.writeValueAsString(card);
            @SuppressWarnings("unchecked")
            Map<String, Object> cardMap = objectMapper.readValue(cardJson, Map.class);
            
            if (cardMap.containsKey("preferredTransport")) {
                metadata.setPreferredTransport((String) cardMap.get("preferredTransport"));
            }
            
            if (cardMap.containsKey("additionalInterfaces")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> interfaces = (List<Map<String, Object>>) cardMap.get("additionalInterfaces");
                if (interfaces != null) {
                    metadata.setAdditionalInterfaces(interfaces);
                }
            }
            
            if (cardMap.containsKey("supportedTransports")) {
                @SuppressWarnings("unchecked")
                List<String> transports = (List<String>) cardMap.get("supportedTransports");
                if (transports != null) {
                    metadata.setSupportedTransports(transports);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract transport protocol info: {}", e.getMessage());
        }
        
        return metadata;
    }
    
    /**
     * Generate a unique agent ID
     */
    private String generateAgentId(String name, String url) {
        String sanitizedName = name.toLowerCase().replaceAll("[^a-z0-9]", "-");
        String hash = Integer.toHexString(url.hashCode());
        return sanitizedName + "-" + hash;
    }
    
    /**
     * Save agent metadata to Redis
     */
    private void saveAgentToRedis(A2AAgentMetadata metadata) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(metadata);
            jedis.set(REDIS_KEY_PREFIX + metadata.getId(), json);
            logger.debug("Saved agent {} to Redis", metadata.getId());
        } catch (Exception e) {
            logger.error("Failed to save agent {} to Redis", metadata.getId(), e);
        }
    }
    
    /**
     * Load agents from Redis on startup
     */
    private void loadAgentsFromRedis() {
        logger.info("Loading A2A agents from Redis...");
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys(REDIS_KEY_PREFIX + "*");
            logger.info("Found {} A2A agents in Redis", keys.size());
            
            for (String key : keys) {
                try {
                    String json = jedis.get(key);
                    if (json != null) {
                        A2AAgentMetadata metadata = objectMapper.readValue(json, A2AAgentMetadata.class);
                        
                        // Try to reconnect to the agent
                        try {
                            AgentCard agentCard = fetchAgentCard(metadata.getUrl());
                            Client client = createClient(agentCard, metadata.getId());
                            clients.put(metadata.getId(), client);
                            agentUrls.put(metadata.getId(), metadata.getUrl());
                            
                            // Update metadata with fresh data
                            A2AAgentMetadata updatedMetadata = convertAgentCardToMetadata(
                                metadata.getId(), agentCard, metadata.getUrl());
                            saveAgentToRedis(updatedMetadata);
                            
                            logger.info("Reconnected to A2A agent: {} ({})", metadata.getName(), metadata.getId());
                        } catch (Exception e) {
                            logger.warn("Failed to reconnect to A2A agent {}: {}", metadata.getName(), e.getMessage());
                            // Keep metadata but mark as disconnected
                            metadata.setStatus("disconnected");
                            saveAgentToRedis(metadata);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to load agent from key {}", key, e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load agents from Redis", e);
        }
    }
    
    /**
     * Close Redis connection pool
     */
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("Closed A2A service Redis connection pool");
        }
    }
}
