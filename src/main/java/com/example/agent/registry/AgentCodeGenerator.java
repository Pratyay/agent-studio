package com.example.agent.registry;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates complete agent code from specification
 */
public class AgentCodeGenerator {
    
    public static class GenerationRequest {
        public String agentName;
        public String description;
        public String instructions;
        public String packageName;
        public List<ToolMetadata> attachedTools;
        public List<String> subagentIds;  // For backward compatibility
        public List<A2AAgentMetadata> subagents;  // Actual subagent metadata
        public String googleApiKey;  // Google AI API key
        public Integer serverPort;  // Port on which agent server will run
        
        // Advanced configuration
        public String model;  // e.g., "gemini-2.5-flash", "gemini-1.5-pro"
        public Double temperature;  // 0.0 to 2.0
        public Integer maxTokens;  // max output tokens
        public Double topP;  // nucleus sampling
        public Integer topK;  // top-k sampling
        
        public GenerationRequest() {
            this.attachedTools = new ArrayList<>();
            this.subagentIds = new ArrayList<>();
            this.subagents = new ArrayList<>();
            // Defaults
            this.model = "gemini-2.5-flash";
            this.temperature = 1.0;
            this.serverPort = 8000;
        }
    }
    
    /**
     * Generate complete agent project as ZIP file
     */
    public byte[] generateAgentZip(GenerationRequest request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Generate Agent Java class
            String agentCode = generateAgentClass(request);
            addZipEntry(zos, "src/main/java/" + packageToPath(request.packageName) + "/" + capitalize(request.agentName) + "Agent.java", agentCode);
            
            // Generate A2A AgentCard producer
            String agentCardCode = generateAgentCardProducer(request);
            addZipEntry(zos, "src/main/java/" + packageToPath(request.packageName) + "/" + capitalize(request.agentName) + "AgentCardProducer.java", agentCardCode);
            
            // Generate A2A AgentExecutor producer
            String agentExecutorCode = generateAgentExecutorProducer(request);
            addZipEntry(zos, "src/main/java/" + packageToPath(request.packageName) + "/" + capitalize(request.agentName) + "AgentExecutorProducer.java", agentExecutorCode);
            
            // Generate pom.xml
            String pomXml = generatePomXml(request);
            addZipEntry(zos, "pom.xml", pomXml);
            
            // Generate README.md
            String readme = generateReadme(request);
            addZipEntry(zos, "README.md", readme);
            
            // Generate config.yml
            String config = generateConfigYml(request);
            addZipEntry(zos, "config.yml", config);
            
            // Generate Quarkus application.properties
            String appProperties = generateApplicationProperties(request);
            addZipEntry(zos, "src/main/resources/application.properties", appProperties);
            
            // Generate .gitignore
            addZipEntry(zos, ".gitignore", generateGitignore());
            
            // Generate startup scripts
            String startScript = generateStartScript(request);
            addZipEntry(zos, "start-server.sh", startScript);
            
            String startScriptBat = generateStartScriptBat(request);
            addZipEntry(zos, "start-server.bat", startScriptBat);
        }
        
        return baos.toByteArray();
    }
    
    private String generateAgentClass(GenerationRequest request) {
        String className = capitalize(request.agentName) + "Agent";
        StringBuilder code = new StringBuilder();
        
        // Determine what tools/agents are needed upfront
        boolean hasMcpTools = !request.attachedTools.isEmpty();
        boolean hasSubAgents = request.subagents != null && !request.subagents.isEmpty();
        
        code.append("package ").append(request.packageName).append(";\n\n");
        code.append("import com.google.adk.agents.BaseAgent;\n");
        code.append("import com.google.adk.agents.LlmAgent;\n");
        code.append("import com.google.adk.tools.mcp.McpTool;\n");
        code.append("import com.google.adk.tools.mcp.McpToolset;\n");
        code.append("import com.google.adk.tools.mcp.SseServerParameters;\n");
        code.append("import com.google.adk.tools.Annotations.Schema;\n");
        code.append("import com.google.adk.tools.FunctionTool;\n");
        code.append("import java.util.*;\n");
        code.append("import java.util.concurrent.*;\n");
        code.append("import java.util.concurrent.TimeoutException;\n");
        code.append("import java.util.function.BiConsumer;\n");
        code.append("import java.util.function.Consumer;\n");
        code.append("import io.a2a.client.Client;\n");
        code.append("import io.a2a.client.ClientEvent;\n");
        code.append("import io.a2a.client.MessageEvent;\n");
        code.append("import io.a2a.client.config.ClientConfig;\n");
        code.append("import io.a2a.client.http.A2ACardResolver;\n");
        code.append("import io.a2a.client.transport.jsonrpc.JSONRPCTransport;\n");
        code.append("import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;\n");
        code.append("import io.a2a.spec.AgentCard;\n");
        code.append("import io.a2a.spec.Message;\n");
        code.append("import io.a2a.spec.Task;\n");
        code.append("import io.a2a.spec.TextPart;\n");
        code.append("import com.google.adk.runner.InMemoryRunner;\n");
        code.append("import com.google.adk.sessions.Session;\n");
        code.append("import com.google.adk.events.Event;\n");
        code.append("import com.google.adk.agents.RunConfig;\n");
        code.append("import com.google.genai.types.Content;\n");
        code.append("import com.google.genai.types.Part;\n");
        code.append("import io.reactivex.rxjava3.core.Flowable;\n\n");
        
        code.append("/**\n");
        code.append(" * ").append(request.description != null ? request.description : "Auto-generated agent").append("\n");
        code.append(" * Generated by Agent Builder\n");
        code.append(" */\n");
        code.append("public class ").append(className).append(" {\n\n");
        
        // Fields
        code.append("    public static BaseAgent ROOT_AGENT;\n");
        
        // Add A2A client cache if there are subagents
        if (hasSubAgents) {
            code.append("    private static final Map<String, Client> a2aClients = new ConcurrentHashMap<>();\n");
        }
        code.append("\n");
        
        // Static initializer
        code.append("    static {\n");
        code.append("        try {\n");
        code.append("            ROOT_AGENT = createAgent();\n");
        code.append("        } catch (Exception e) {\n");
        code.append("            throw new RuntimeException(\"Failed to initialize agent\", e);\n");
        code.append("        }\n");
        code.append("    }\n\n");
        
        // Create agent
        code.append("    private static BaseAgent createAgent() {\n");
        code.append("        try {\n");
        
        // Initialize all tools (MCP toolsets + A2A sub-agent tools)
        if (hasMcpTools || hasSubAgents) {
            code.append("            // Initialize all tools\n");
            code.append("            List<Object> allTools = new ArrayList<>();\n\n");
        }
        
        // Initialize MCP toolsets
        if (hasMcpTools) {
            code.append("            // Add MCP toolsets\n");
            for (ToolMetadata tool : request.attachedTools) {
                String toolVar = sanitizeVarName(tool.getName());
                code.append("            SseServerParameters ").append(toolVar).append("Params = \n");
                code.append("                SseServerParameters.builder()\n");
                code.append("                    .url(\"").append(tool.getEndpointUrl()).append("\")\n");
                code.append("                    .build();\n");
                code.append("            McpToolset ").append(toolVar).append("Toolset = new McpToolset(").append(toolVar).append("Params);\n");
                code.append("            allTools.add(").append(toolVar).append("Toolset);\n");
                code.append("            System.out.println(\"Registered MCP toolset: ").append(tool.getName()).append("\");\n\n");
            }
        }
        
        // Initialize A2A sub-agent tools
        if (hasSubAgents) {
            code.append("            // Add A2A sub-agent tools\n");
            for (A2AAgentMetadata subagent : request.subagents) {
                String subagentVar = sanitizeVarName(subagent.getName());
                code.append("            FunctionTool ").append(subagentVar).append("Tool = FunctionTool.create(\n");
                code.append("                ").append(className).append(".class,\n");
                code.append("                \"call").append(capitalize(subagentVar)).append("\"\n");
                code.append("            );\n");
                code.append("            allTools.add(").append(subagentVar).append("Tool);\n");
                code.append("            System.out.println(\"Registered A2A agent tool: ").append(subagent.getName()).append("\");\n\n");
            }
        }
        
        code.append("            return LlmAgent.builder()\n");
        code.append("                .name(\"").append(sanitizeVarName(request.agentName)).append("\")\n");
        code.append("                .description(\"").append(request.description != null ? request.description.replace("\"", "\\\"") : "Generated agent with MCP tools").append("\")\n");
        
        // Add instructions - enhanced with tool and subagent information
        StringBuilder instructionsBuilder = new StringBuilder();
        if (request.instructions != null && !request.instructions.trim().isEmpty()) {
            instructionsBuilder.append(request.instructions.replace("\"", "\\\""));
        } else {
            instructionsBuilder.append("You are a helpful AI assistant with access to various tools and capabilities.");
        }
        
        // Add MCP tool information to instructions
        if (!request.attachedTools.isEmpty()) {
            instructionsBuilder.append("\n\nYou have access to the following MCP tool servers:\n");
            for (ToolMetadata tool : request.attachedTools) {
                instructionsBuilder.append("- ").append(tool.getName()).append(": ").append(tool.getEndpointUrl()).append("\n");
            }
            instructionsBuilder.append("\nUse the appropriate MCP tools when needed to complete user requests.");
        }
        
        // Add subagent information to instructions
        if (request.subagents != null && !request.subagents.isEmpty()) {
            instructionsBuilder.append("\n\nYou can delegate tasks to the following sub-agents:\n");
            for (A2AAgentMetadata subagent : request.subagents) {
                instructionsBuilder.append("- ").append(subagent.getName()).append(": ").append(subagent.getDescription() != null ? subagent.getDescription() : "Available for delegation").append("\n");
            }
            instructionsBuilder.append("\n***CRITICAL INSTRUCTION***: When you delegate to a sub-agent, you will receive their response in a structured format with 'agent', 'status', and 'response' fields. ");
            instructionsBuilder.append("You MUST respond to the user with the information from the sub-agent. Do NOT leave the user waiting. ");
            instructionsBuilder.append("Always relay the sub-agent's response to the user in a natural, conversational way. ");
            instructionsBuilder.append("If the sub-agent cannot help, offer alternative assistance based on your own capabilities.");
        }
        
        String instructions = instructionsBuilder.toString();
        code.append("                .instruction(\"\"\"\n");
        code.append("                    ").append(instructions.replace("\n", "\n                    ")).append("\n");
        code.append("                    \"\"\")\n");
        code.append("                .model(\"").append(request.model != null ? request.model : "gemini-2.5-flash").append("\")\n");
        
        // Register all tools with the agent
        if (hasMcpTools || hasSubAgents) {
            code.append("                .tools(allTools)  // Register all MCP toolsets and sub-agent tools\n");
        }
        
        // Note: temperature, topP, topK, and maxTokens are not supported in Google ADK 0.3.0
        
        code.append("                .build();\n\n");
        code.append("        } catch (Exception e) {\n");
        code.append("            throw new RuntimeException(\"Failed to initialize MCP tools\", e);\n");
        code.append("        }\n");
        code.append("    }\n\n");
        
        // Add A2A agent caller methods if sub-agents exist
        if (request.subagents != null && !request.subagents.isEmpty()) {
            // Create wrapper methods for each sub-agent
            for (A2AAgentMetadata subagent : request.subagents) {
                String subagentVar = sanitizeVarName(subagent.getName());
                String subagentUrl = subagent.getUrl();
                
                code.append("    /**\n");
                code.append("     * Call sub-agent: ").append(subagent.getName()).append("\n");
                code.append("     */\n");
                code.append("    @Schema(description = \"Delegate task to ").append(subagent.getName()).append("\")\n");
                code.append("    public static Map<String, Object> call").append(capitalize(subagentVar)).append("(\n");
                code.append("        @Schema(name = \"task\", description = \"Task to delegate\") String task) {\n");
                code.append("        String response = callA2AAgent(\"").append(subagentUrl).append("\", task);\n");
                code.append("        return Map.of(\n");
                code.append("            \"agent\", \"").append(subagent.getName()).append("\",\n");
                code.append("            \"status\", \"success\",\n");
                code.append("            \"response\", response\n");
                code.append("        );\n");
                code.append("    }\n\n");
            }
            
            // A2A state management - one consumer per agent that handles all calls
            code.append("    private static final Map<String, CompletableFuture<String>> a2aResponseFutures = new ConcurrentHashMap<>();\n\n");
            code.append("    /**\n");
            code.append("     * Get or create an A2A Client for a given agent URL with shared consumer\n");
            code.append("     */\n");
            code.append("    private static Client getA2AClient(String agentUrl) {\n");
            code.append("        return a2aClients.computeIfAbsent(agentUrl, url -> {\n");
            code.append("            try {\n");
            code.append("                System.out.println(\"[A2A] Creating new client for: \" + url);\n");
            code.append("                A2ACardResolver resolver = new A2ACardResolver(url);\n");
            code.append("                AgentCard agentCard = resolver.getAgentCard();\n\n");
            code.append("                ClientConfig clientConfig = new ClientConfig.Builder()\n");
            code.append("                    .setAcceptedOutputModes(List.of(\"text\"))\n");
            code.append("                    .build();\n\n");
            code.append("                // Create shared consumer that handles all events for all calls to this agent\n");
            code.append("                List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();\n");
            code.append("                consumers.add((event, card) -> {\n");
            code.append("                    System.out.println(\"[A2A Event] Received: \" + event.getClass().getSimpleName());\n");
            code.append("                    \n");
            code.append("                    // Try to find a pending future that isn't done yet\n");
            code.append("                    CompletableFuture<String> targetFuture = null;\n");
            code.append("                    String targetCallId = null;\n");
            code.append("                    for (Map.Entry<String, CompletableFuture<String>> entry : a2aResponseFutures.entrySet()) {\n");
            code.append("                        if (!entry.getValue().isDone()) {\n");
            code.append("                            targetFuture = entry.getValue();\n");
            code.append("                            targetCallId = entry.getKey();\n");
            code.append("                            break;\n");
            code.append("                        }\n");
            code.append("                    }\n");
            code.append("                    \n");
            code.append("                    if (targetFuture == null) {\n");
            code.append("                        System.out.println(\"[A2A Event] No pending future found\");\n");
            code.append("                        return;\n");
            code.append("                    }\n");
            code.append("                    \n");
            code.append("                    System.out.println(\"[A2A Event] Processing for callId: \" + targetCallId);\n");
            code.append("                    StringBuilder textBuilder = new StringBuilder();\n\n");
            code.append("                    // Handle MessageEvent\n");
            code.append("                    if (event instanceof MessageEvent messageEvent) {\n");
            code.append("                        Message responseMessage = messageEvent.getMessage();\n");
            code.append("                        if (responseMessage.getParts() != null) {\n");
            code.append("                            for (io.a2a.spec.Part<?> part : responseMessage.getParts()) {\n");
            code.append("                                if (part instanceof TextPart textPart) {\n");
            code.append("                                    textBuilder.append(textPart.getText());\n");
            code.append("                                }\n");
            code.append("                            }\n");
            code.append("                        }\n");
            code.append("                    }\n\n");
            code.append("                    // Try to extract task and artifacts from TaskUpdateEvent\n");
            code.append("                    try {\n");
            code.append("                        var getTask = event.getClass().getMethod(\"getTask\");\n");
            code.append("                        Task task = (Task) getTask.invoke(event);\n");
            code.append("                        if (task != null && task.getArtifacts() != null) {\n");
            code.append("                            System.out.println(\"[A2A Event] Task has \" + task.getArtifacts().size() + \" artifacts\");\n");
            code.append("                            for (var artifact : task.getArtifacts()) {\n");
            code.append("                                if (artifact.parts() != null) {\n");
            code.append("                                    for (io.a2a.spec.Part<?> part : artifact.parts()) {\n");
            code.append("                                        if (part instanceof TextPart textPart) {\n");
            code.append("                                            textBuilder.append(textPart.getText());\n");
            code.append("                                            System.out.println(\"[A2A Event] Extracted artifact text: \" + textPart.getText());\n");
            code.append("                                        }\n");
            code.append("                                    }\n");
            code.append("                                }\n");
            code.append("                            }\n");
            code.append("                        }\n");
            code.append("                    } catch (Exception e) {\n");
            code.append("                        System.out.println(\"[A2A Event] Could not get task: \" + e.getMessage());\n");
            code.append("                    }\n\n");
            code.append("                    if (textBuilder.length() > 0) {\n");
            code.append("                        String responseText = textBuilder.toString();\n");
            code.append("                        System.out.println(\"[A2A Event] Completing future with response: \" + responseText);\n");
            code.append("                        if (!targetFuture.isDone()) {\n");
            code.append("                            targetFuture.complete(responseText);\n");
            code.append("                        }\n");
            code.append("                    }\n");
            code.append("                });\n\n");
            code.append("                // Create error handler\n");
            code.append("                Consumer<Throwable> errorHandler = (error) -> {\n");
            code.append("                    System.out.println(\"[A2A Error] \" + error.getClass().getSimpleName() + \": \" + error.getMessage());\n");
            code.append("                };\n\n");
            code.append("                // Build client with shared consumer\n");
            code.append("                Client client = Client\n");
            code.append("                    .builder(agentCard)\n");
            code.append("                    .clientConfig(clientConfig)\n");
            code.append("                    .addConsumers(consumers)\n");
            code.append("                    .streamingErrorHandler(errorHandler)\n");
            code.append("                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())\n");
            code.append("                    .build();\n\n");
            code.append("                System.out.println(\"[A2A] Client created successfully with shared consumer\");\n");
            code.append("                return client;\n");
            code.append("            } catch (Exception e) {\n");
            code.append("                System.out.println(\"[A2A] Error creating client: \" + e.getMessage());\n");
            code.append("                e.printStackTrace();\n");
            code.append("                throw new RuntimeException(\"Failed to create A2A client: \" + e.getMessage(), e);\n");
            code.append("            }\n");
            code.append("        });\n");
            code.append("    }\n\n");
            code.append("    /**\n");
            code.append("     * Helper method to call A2A agents\n");
            code.append("     */\n");
            code.append("    private static String callA2AAgent(String agentUrl, String messageText) {\n");
            code.append("        System.out.println(\"[A2A] Calling agent at \" + agentUrl + \" with message: \" + messageText);\n");
            code.append("        try {\n");
            code.append("            // Create a unique call ID for this request\n");
            code.append("            String callId = UUID.randomUUID().toString();\n");
            code.append("            CompletableFuture<String> responseFuture = new CompletableFuture<>();\n");
            code.append("            a2aResponseFutures.put(callId, responseFuture);\n\n");
            code.append("            // Get or create client\n");
            code.append("            Client client = getA2AClient(agentUrl);\n\n");
            code.append("            // Create and send message\n");
            code.append("            TextPart textPart = new TextPart(messageText, null);\n");
            code.append("            List<io.a2a.spec.Part<?>> parts = List.of(textPart);\n");
            code.append("            String messageId = UUID.randomUUID().toString();\n\n");
            code.append("            Message message = new Message(\n");
            code.append("                Message.Role.USER,\n");
            code.append("                parts,\n");
            code.append("                messageId,\n");
            code.append("                null, null, null, null\n");
            code.append("            );\n\n");
            code.append("            System.out.println(\"[A2A] Sending message with callId: \" + callId);\n");
            code.append("            client.sendMessage(message);\n");
            code.append("            System.out.println(\"[A2A] Message sent, waiting for response...\");\n\n");
            code.append("            try {\n");
            code.append("                // Wait for response\n");
            code.append("                String response = responseFuture.get(60, TimeUnit.SECONDS);\n");
            code.append("                System.out.println(\"[A2A] Got final response: \" + response);\n");
            code.append("                return response;\n");
            code.append("            } catch (TimeoutException e) {\n");
            code.append("                System.err.println(\"[A2A] Timeout after 60s waiting for response\");\n");
            code.append("                return \"Error: Timeout waiting for response from sub-agent (60s)\";\n");
            code.append("            } finally {\n");
            code.append("                // Clean up the future from the map\n");
            code.append("                a2aResponseFutures.remove(callId);\n");
            code.append("                System.out.println(\"[A2A] Removed callId from futures map: \" + callId);\n");
            code.append("            }\n");
            code.append("        } catch (Exception e) {\n");
            code.append("            e.printStackTrace();\n");
            code.append("            return \"Error calling sub-agent: \" + e.getMessage();\n");
            code.append("        }\n");
            code.append("    }\n\n");
        }
        
        // Chat method for executor
        code.append("    public static String chat(String message) {\n");
        code.append("        try {\n");
        code.append("            System.out.println(\"[Chat] Received message: \" + message);\n");
        code.append("            // Create runner and session\n");
        code.append("            InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);\n");
        code.append("            Session session = runner\n");
        code.append("                .sessionService()\n");
        code.append("                .createSession(runner.appName(), \"user\")\n");
        code.append("                .blockingGet();\n\n");
        code.append("            // Create user message\n");
        code.append("            Content userMsg = Content.fromParts(Part.fromText(message));\n");
        code.append("            RunConfig runConfig = RunConfig.builder().build();\n\n");
        code.append("            // Execute and collect ALL events - use blockingSubscribe\n");
        code.append("            Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);\n");
        code.append("            StringBuilder response = new StringBuilder();\n");
        code.append("            java.util.concurrent.atomic.AtomicBoolean foundResponse = new java.util.concurrent.atomic.AtomicBoolean(false);\n\n");
        code.append("            System.out.println(\"[Chat] Starting to process event stream...\");\n");
        code.append("            try {\n");
        code.append("                // Use timeout with blockingSubscribe to prevent hanging\n");
        code.append("                events\n");
        code.append("                    .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())\n");
        code.append("                    .timeout(90, TimeUnit.SECONDS)\n");
        code.append("                    .blockingSubscribe(\n");
        code.append("                        event -> {\n");
        code.append("                            System.out.println(\"[Chat Event] Type: \" + event.getClass().getSimpleName() + \", Final: \" + event.finalResponse());\n");
        code.append("                            // Log all event content for debugging\n");
        code.append("                            try {\n");
        code.append("                                String content = event.stringifyContent();\n");
        code.append("                                if (content != null && !content.trim().isEmpty()) {\n");
        code.append("                                    System.out.println(\"[Chat Event] Content: \" + content.substring(0, Math.min(200, content.length())));\n");
        code.append("                                }\n");
        code.append("                            } catch (Exception e) {}\n");
        code.append("                            \n");
        code.append("                            if (event.finalResponse()) {\n");
        code.append("                                String content = event.stringifyContent();\n");
        code.append("                                System.out.println(\"[Chat Event] FINAL response content: \" + content);\n");
        code.append("                                if (content != null && !content.isEmpty()) {\n");
        code.append("                                    response.append(content);\n");
        code.append("                                    foundResponse.set(true);\n");
        code.append("                                }\n");
        code.append("                            }\n");
        code.append("                        },\n");
        code.append("                        error -> {\n");
        code.append("                            System.err.println(\"[Chat] Stream error: \" + error.getMessage());\n");
        code.append("                            error.printStackTrace();\n");
        code.append("                        },\n");
        code.append("                        () -> System.out.println(\"[Chat] Stream completed\")\n");
        code.append("                    );\n");
        code.append("            } catch (Exception e) {\n");
        code.append("                System.err.println(\"[Chat] Exception during event processing: \" + e.getMessage());\n");
        code.append("                e.printStackTrace();\n");
        code.append("            }\n\n");
        code.append("            String result = response.toString();\n");
        code.append("            if (!foundResponse.get() || result.isEmpty()) {\n");
        code.append("                System.out.println(\"[Chat] WARNING: No final response found!\");\n");
        code.append("                return \"I processed your request but did not generate a response. Please try again.\";\n");
        code.append("            }\n");
        code.append("            System.out.println(\"[Chat] Returning response: \" + result);\n");
        code.append("            return result;\n");
        code.append("        } catch (Exception e) {\n");
        code.append("            System.err.println(\"[Chat] Error: \" + e.getMessage());\n");
        code.append("            e.printStackTrace();\n");
        code.append("            return \"Error: \" + e.getMessage();\n");
        code.append("        }\n");
        code.append("    }\n\n");
        
        // Main method
        code.append("    public static void main(String[] args) {\n");
        code.append("        System.out.println(\"").append(className).append(" initialized successfully\");\n");
        code.append("        System.out.println(\"Agent ready!\");\n");
        code.append("        System.out.println(\"To run as A2A server, use: mvn quarkus:dev\");\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    private String generatePomXml(GenerationRequest request) {
        String artifactId = request.agentName.toLowerCase().replace(" ", "-") + "-agent";
        
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
               "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
               "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
               "    <modelVersion>4.0.0</modelVersion>\n\n" +
               "    <groupId>" + request.packageName + "</groupId>\n" +
               "    <artifactId>" + artifactId + "</artifactId>\n" +
               "    <version>1.0.0</version>\n\n" +
               "    <properties>\n" +
               "        <maven.compiler.source>17</maven.compiler.source>\n" +
               "        <maven.compiler.target>17</maven.compiler.target>\n" +
               "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
               "        <quarkus.version>3.8.1</quarkus.version>\n" +
               "        <a2a.sdk.version>0.3.1.Final</a2a.sdk.version>\n" +
               "    </properties>\n\n" +
               "    <dependencyManagement>\n" +
               "        <dependencies>\n" +
               "            <!-- Quarkus BOM -->\n" +
               "            <dependency>\n" +
               "                <groupId>io.quarkus</groupId>\n" +
               "                <artifactId>quarkus-bom</artifactId>\n" +
               "                <version>${quarkus.version}</version>\n" +
               "                <type>pom</type>\n" +
               "                <scope>import</scope>\n" +
               "            </dependency>\n" +
               "            <!-- MCP BOM -->\n" +
               "            <dependency>\n" +
               "                <groupId>io.modelcontextprotocol.sdk</groupId>\n" +
               "                <artifactId>mcp-bom</artifactId>\n" +
               "                <version>0.12.1</version>\n" +
               "                <type>pom</type>\n" +
               "                <scope>import</scope>\n" +
               "            </dependency>\n" +
               "        </dependencies>\n" +
               "    </dependencyManagement>\n\n" +
               "    <dependencies>\n" +
               "        <!-- Quarkus -->\n" +
               "        <dependency>\n" +
               "            <groupId>io.quarkus</groupId>\n" +
               "            <artifactId>quarkus-arc</artifactId>\n" +
               "        </dependency>\n" +
               "        <dependency>\n" +
               "            <groupId>io.quarkus</groupId>\n" +
               "            <artifactId>quarkus-resteasy-reactive</artifactId>\n" +
               "        </dependency>\n\n" +
               "        <!-- A2A Java SDK Client -->\n" +
               "        <dependency>\n" +
               "            <groupId>io.github.a2asdk</groupId>\n" +
               "            <artifactId>a2a-java-sdk-client</artifactId>\n" +
               "            <version>${a2a.sdk.version}</version>\n" +
               "        </dependency>\n\n" +
               "        <!-- A2A Client Transport - JSON-RPC -->\n" +
               "        <dependency>\n" +
               "            <groupId>io.github.a2asdk</groupId>\n" +
               "            <artifactId>a2a-java-sdk-client-transport-jsonrpc</artifactId>\n" +
               "            <version>${a2a.sdk.version}</version>\n" +
               "        </dependency>\n\n" +
               "        <!-- A2A Client Transport - REST -->\n" +
               "        <dependency>\n" +
               "            <groupId>io.github.a2asdk</groupId>\n" +
               "            <artifactId>a2a-java-sdk-client-transport-rest</artifactId>\n" +
               "            <version>${a2a.sdk.version}</version>\n" +
               "        </dependency>\n\n" +
               "        <!-- A2A Java SDK Server (for exposing agent via A2A) -->\n" +
               "        <dependency>\n" +
               "            <groupId>io.github.a2asdk</groupId>\n" +
               "            <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>\n" +
               "            <version>${a2a.sdk.version}</version>\n" +
               "        </dependency>\n\n" +
               "        <dependency>\n" +
               "            <groupId>io.github.a2asdk</groupId>\n" +
               "            <artifactId>a2a-java-sdk-reference-rest</artifactId>\n" +
               "            <version>${a2a.sdk.version}</version>\n" +
               "        </dependency>\n\n" +
               "        <!-- Google ADK -->\n" +
               "        <dependency>\n" +
               "            <groupId>com.google.adk</groupId>\n" +
               "            <artifactId>google-adk</artifactId>\n" +
               "            <version>0.3.0</version>\n" +
               "        </dependency>\n\n" +
               "        <!-- MCP Java SDK -->\n" +
               "        <dependency>\n" +
               "            <groupId>io.modelcontextprotocol.sdk</groupId>\n" +
               "            <artifactId>mcp</artifactId>\n" +
               "        </dependency>\n" +
               "    </dependencies>\n\n" +
               "    <build>\n" +
               "        <plugins>\n" +
               "            <plugin>\n" +
               "                <groupId>io.quarkus</groupId>\n" +
               "                <artifactId>quarkus-maven-plugin</artifactId>\n" +
               "                <version>${quarkus.version}</version>\n" +
               "                <executions>\n" +
               "                    <execution>\n" +
               "                        <goals>\n" +
               "                            <goal>build</goal>\n" +
               "                        </goals>\n" +
               "                    </execution>\n" +
               "                </executions>\n" +
               "            </plugin>\n" +
               "            <plugin>\n" +
               "                <groupId>org.apache.maven.plugins</groupId>\n" +
               "                <artifactId>maven-compiler-plugin</artifactId>\n" +
               "                <version>3.11.0</version>\n" +
               "            </plugin>\n" +
               "        </plugins>\n" +
               "    </build>\n" +
               "</project>\n";
    }
    
    private String generateReadme(GenerationRequest request) {
        String className = capitalize(request.agentName) + "Agent";
        StringBuilder readme = new StringBuilder();
        
        readme.append("# ").append(className).append("\n\n");
        readme.append(request.description != null ? request.description : "Auto-generated AI agent").append("\n\n");
        
        // Add instructions if provided
        if (request.instructions != null && !request.instructions.trim().isEmpty()) {
            readme.append("## Instructions\n\n");
            readme.append(request.instructions).append("\n\n");
        }
        
        readme.append("## Overview\n\n");
        
        if (request.attachedTools != null && !request.attachedTools.isEmpty()) {
            readme.append("This agent includes the following MCP tools:\n\n");
            for (ToolMetadata tool : request.attachedTools) {
                readme.append("- **").append(tool.getName()).append("**: ").append(tool.getEndpointUrl()).append("\n");
            }
            readme.append("\n");
        }
        
        if (request.subagents != null && !request.subagents.isEmpty()) {
            readme.append("This agent can delegate to the following sub-agents:\n\n");
            for (A2AAgentMetadata subagent : request.subagents) {
                readme.append("- **").append(subagent.getName()).append("**: ")
                      .append(subagent.getDescription() != null ? subagent.getDescription() : "Sub-agent")
                      .append(" (").append(subagent.getUrl()).append(")\n");
                if (subagent.getSkills() != null && !subagent.getSkills().isEmpty()) {
                    readme.append("  - Skills: ");
                    int skillCount = Math.min(3, subagent.getSkills().size());
                    for (int i = 0; i < skillCount; i++) {
                        Map<String, Object> skill = subagent.getSkills().get(i);
                        if (i > 0) readme.append(", ");
                        readme.append(skill.get("name") != null ? skill.get("name") : "Skill " + (i+1));
                    }
                    if (subagent.getSkills().size() > 3) {
                        readme.append(", and ").append(subagent.getSkills().size() - 3).append(" more");
                    }
                    readme.append("\n");
                }
            }
            readme.append("\n");
        }
        
        readme.append("\n## Prerequisites\n\n");
        readme.append("- Java 17 or higher\n");
        readme.append("- Maven 3.6+\n");
        readme.append("- Google AI API key (Gemini)\n");
        readme.append("- MCP servers running at the configured endpoints\n\n");
        
        readme.append("## Setup\n\n");
        readme.append("1. Set your Google AI API key:\n");
        readme.append("   ```bash\n");
        readme.append("   export GOOGLE_API_KEY=your-api-key-here\n");
        readme.append("   ```\n\n");
        readme.append("   Get your API key from: https://aistudio.google.com/app/apikey\n\n");
        
        readme.append("2. Ensure MCP servers are running:\n");
        for (ToolMetadata tool : request.attachedTools) {
            readme.append("   - ").append(tool.getName()).append(": ").append(tool.getEndpointUrl()).append("\n");
        }
        
        readme.append("\n## Build\n\n");
        readme.append("```bash\n");
        readme.append("mvn clean package\n");
        readme.append("```\n\n");
        
        readme.append("## Run as A2A Server\n\n");
        readme.append("The agent includes an A2A (Agent-to-Agent) server using Quarkus:\n\n");
        readme.append("### Development Mode\n\n");
        readme.append("```bash\n");
        readme.append("# Run in dev mode with live reload\n");
        readme.append("mvn quarkus:dev\n");
        readme.append("```\n\n");
        readme.append("### Production Mode\n\n");
        readme.append("```bash\n");
        readme.append("# Build the application\n");
        readme.append("mvn clean package\n\n");
        readme.append("# Run the native executable or JAR\n");
        readme.append("java -jar target/quarkus-app/quarkus-run.jar\n");
        readme.append("```\n\n");
        int port = request.serverPort != null ? request.serverPort : 8000;
        readme.append("The agent will be accessible at `http://localhost:").append(port).append("` by default.\n\n");
        readme.append("### Custom Port\n\n");
        readme.append("To run on a different port, set the `quarkus.http.port` property:\n\n");
        readme.append("```bash\n");
        readme.append("mvn quarkus:dev -Dquarkus.http.port=9000\n");
        readme.append("```\n\n");
        
        readme.append("## A2A Server Features\n\n");
        readme.append("The generated agent is a fully functional A2A server with:\n\n");
        readme.append("- **JSON-RPC 2.0 transport**: Standard JSON-RPC interface\n");
        readme.append("- **REST transport**: HTTP+JSON/REST API\n");
        readme.append("- **Agent Card**: Published metadata about agent capabilities\n");
        readme.append("- **Agent Executor**: Handles task execution and lifecycle\n\n");
        readme.append("### How it works\n\n");
        readme.append("The agent automatically:\n");
        readme.append("1. Connects to configured MCP servers\n");
        readme.append("2. Discovers available tools\n");
        readme.append("3. Integrates them with the Gemini model\n");
        readme.append("4. Exposes an A2A-compliant server that other agents can connect to\n\n");
        readme.append("### API Endpoints\n\n");
        readme.append("- `GET /agent-card`: Get agent metadata\n");
        readme.append("- `POST /tasks`: Create a new task\n");
        readme.append("- `GET /tasks/{taskId}`: Get task status\n");
        readme.append("- `DELETE /tasks/{taskId}`: Cancel a task\n\n");
        readme.append("### Programmatic Integration\n\n");
        readme.append("You can also integrate this agent into your own application using the ROOT_AGENT field:\n\n");
        readme.append("```java\n");
        readme.append("import ").append(request.packageName).append(".").append(capitalize(request.agentName)).append("Agent;\n\n");
        readme.append("BaseAgent agent = ").append(capitalize(request.agentName)).append("Agent.ROOT_AGENT;\n");
        readme.append("// Use the agent in your application\n");
        readme.append("```\n\n");
        readme.append("---\n");
        readme.append("*Generated by Agent Builder*\n");
        
        return readme.toString();
    }
    
    private String generateConfigYml(GenerationRequest request) {
        return "# Agent Configuration\n" +
               "agent:\n" +
               "  name: " + request.agentName + "\n" +
               "  description: " + (request.description != null ? request.description : "") + "\n\n" +
               "# MCP Tool Endpoints\n" +
               "tools:\n" +
               request.attachedTools.stream()
                   .map(t -> "  - name: " + t.getName() + "\n    endpoint: " + t.getEndpointUrl())
                   .reduce("", (a, b) -> a + b + "\n");
    }
    
    private String generateGitignore() {
        return "target/\n" +
               ".idea/\n" +
               "*.iml\n" +
               ".DS_Store\n" +
               "*.class\n" +
               "*.log\n";
    }
    
    private void addZipEntry(ZipOutputStream zos, String filename, String content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content.getBytes());
        zos.closeEntry();
    }
    
    private String packageToPath(String packageName) {
        return packageName.replace(".", "/");
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
    }
    
    private String sanitizeVarName(String name) {
        return name.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
    }
    
    private String generateAgentCardProducer(GenerationRequest request) {
        String className = capitalize(request.agentName) + "AgentCardProducer";
        String agentName = request.agentName;
        StringBuilder code = new StringBuilder();
        
        code.append("package ").append(request.packageName).append(";\n\n");
        code.append("import io.a2a.server.PublicAgentCard;\n");
        code.append("import io.a2a.spec.AgentCapabilities;\n");
        code.append("import io.a2a.spec.AgentCard;\n");
        code.append("import io.a2a.spec.AgentSkill;\n");
        code.append("import jakarta.enterprise.context.ApplicationScoped;\n");
        code.append("import jakarta.enterprise.inject.Produces;\n");
        code.append("import java.util.*;\n\n");
        
        code.append("/**\n");
        code.append(" * A2A Agent Card Producer for ").append(agentName).append("\n");
        code.append(" * Defines the agent's capabilities and metadata\n");
        code.append(" */\n");
        code.append("@ApplicationScoped\n");
        code.append("public class ").append(className).append(" {\n\n");
        
        code.append("    @Produces\n");
        code.append("    @PublicAgentCard\n");
        code.append("    public AgentCard agentCard() {\n");
        code.append("        return new AgentCard.Builder()\n");
        code.append("            .name(\"").append(agentName).append("\")\n");
        code.append("            .description(\"").append(request.description != null ? request.description.replace("\"", "\\\"") : "AI Agent with MCP tools").append("\")\n");
        code.append("            .url(\"http://localhost:").append(request.serverPort != null ? request.serverPort : 8000).append("\")\n");
        code.append("            .version(\"1.0.0\")\n");
        code.append("            .capabilities(new AgentCapabilities.Builder()\n");
        code.append("                .streaming(true)\n");
        code.append("                .pushNotifications(false)\n");
        code.append("                .stateTransitionHistory(false)\n");
        code.append("                .build())\n");
        code.append("            .defaultInputModes(Collections.singletonList(\"text\"))\n");
        code.append("            .defaultOutputModes(Collections.singletonList(\"text\"))\n");
        code.append("            .skills(createSkills())\n");
        code.append("            .protocolVersion(\"0.3.0\")\n");
        code.append("            .build();\n");
        code.append("    }\n\n");
        
        code.append("    private List<AgentSkill> createSkills() {\n");
        code.append("        List<AgentSkill> skills = new ArrayList<>();\n\n");
        
        // Add skills based on attached tools
        for (ToolMetadata tool : request.attachedTools) {
            String skillId = sanitizeVarName(tool.getName()) + "_skill";
            code.append("        skills.add(new AgentSkill.Builder()\n");
            code.append("            .id(\"").append(skillId).append("\")\n");
            code.append("            .name(\"").append(tool.getName()).append("\")\n");
            code.append("            .description(\"Access to ").append(tool.getName()).append(" MCP tools\")\n");
            code.append("            .tags(List.of(\"").append(sanitizeVarName(tool.getName())).append("\"))\n");
            code.append("            .examples(List.of(\"Use ").append(tool.getName()).append(" tools\"))\n");
            code.append("            .build());\n\n");
        }
        
        code.append("        return skills;\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    private String generateAgentExecutorProducer(GenerationRequest request) {
        String className = capitalize(request.agentName) + "AgentExecutorProducer";
        String agentClassName = capitalize(request.agentName) + "Agent";
        StringBuilder code = new StringBuilder();
        
        code.append("package ").append(request.packageName).append(";\n\n");
        code.append("import io.a2a.server.agentexecution.AgentExecutor;\n");
        code.append("import io.a2a.server.agentexecution.RequestContext;\n");
        code.append("import io.a2a.server.events.EventQueue;\n");
        code.append("import io.a2a.server.tasks.TaskUpdater;\n");
        code.append("import io.a2a.spec.*;\n");
        code.append("import jakarta.enterprise.context.ApplicationScoped;\n");
        code.append("import jakarta.enterprise.inject.Produces;\n");
        code.append("import jakarta.inject.Inject;\n");
        code.append("import java.util.*;\n\n");
        
        code.append("/**\n");
        code.append(" * A2A Agent Executor Producer\n");
        code.append(" * Handles task execution and lifecycle management\n");
        code.append(" */\n");
        code.append("@ApplicationScoped\n");
        code.append("public class ").append(className).append(" {\n\n");
        
        code.append("    @Produces\n");
        code.append("    public AgentExecutor agentExecutor() {\n");
        code.append("        return new ").append(agentClassName).append("Executor();\n");
        code.append("    }\n\n");
        
        code.append("    private static class ").append(agentClassName).append("Executor implements AgentExecutor {\n\n");
        
        code.append("        @Override\n");
        code.append("        public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {\n");
        code.append("            System.out.println(\"[Executor] Starting task execution\");\n");
        code.append("            TaskUpdater updater = new TaskUpdater(context, eventQueue);\n\n");
        
        code.append("            // Mark the task as submitted and start working on it\n");
        code.append("            if (context.getTask() == null) {\n");
        code.append("                updater.submit();\n");
        code.append("            }\n");
        code.append("            updater.startWork();\n\n");
        
        code.append("            // Extract the text from the message\n");
        code.append("            String userMessage = extractTextFromMessage(context.getMessage());\n");
        code.append("            System.out.println(\"[Executor] User message: \" + userMessage);\n\n");
        
        code.append("            // Call the agent with the user's message\n");
        code.append("            System.out.println(\"[Executor] Calling agent chat method...\");\n");
        code.append("            String response = ").append(agentClassName).append(".chat(userMessage);\n");
        code.append("            System.out.println(\"[Executor] Agent response: \" + response);\n\n");
        
        code.append("            // Create the response part\n");
        code.append("            TextPart responsePart = new TextPart(response, null);\n");
        code.append("            List<Part<?>> parts = List.of(responsePart);\n\n");
        
        code.append("            // Add the response as an artifact and complete the task\n");
        code.append("            System.out.println(\"[Executor] Adding artifact and completing task\");\n");
        code.append("            updater.addArtifact(parts, null, null, null);\n");
        code.append("            updater.complete();\n");
        code.append("            System.out.println(\"[Executor] Task completed\");\n");
        code.append("        }\n\n");
        
        code.append("        @Override\n");
        code.append("        public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {\n");
        code.append("            Task task = context.getTask();\n\n");
        
        code.append("            if (task.getStatus().state() == TaskState.CANCELED) {\n");
        code.append("                throw new TaskNotCancelableError();\n");
        code.append("            }\n\n");
        
        code.append("            if (task.getStatus().state() == TaskState.COMPLETED) {\n");
        code.append("                throw new TaskNotCancelableError();\n");
        code.append("            }\n\n");
        
        code.append("            // Cancel the task\n");
        code.append("            TaskUpdater updater = new TaskUpdater(context, eventQueue);\n");
        code.append("            updater.cancel();\n");
        code.append("        }\n\n");
        
        code.append("        private String extractTextFromMessage(Message message) {\n");
        code.append("            StringBuilder textBuilder = new StringBuilder();\n");
        code.append("            if (message.getParts() != null) {\n");
        code.append("                for (Part part : message.getParts()) {\n");
        code.append("                    if (part instanceof TextPart textPart) {\n");
        code.append("                        textBuilder.append(textPart.getText());\n");
        code.append("                    }\n");
        code.append("                }\n");
        code.append("            }\n");
        code.append("            return textBuilder.toString();\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    private String generateAgentCardResource(GenerationRequest request) {
        String className = capitalize(request.agentName) + "CardResource";
        StringBuilder code = new StringBuilder();
        
        code.append("package ").append(request.packageName).append(";\n\n");
        code.append("import io.a2a.server.PublicAgentCard;\n");
        code.append("import io.a2a.spec.AgentCard;\n");
        code.append("import jakarta.inject.Inject;\n");
        code.append("import jakarta.ws.rs.GET;\n");
        code.append("import jakarta.ws.rs.Path;\n");
        code.append("import jakarta.ws.rs.Produces;\n");
        code.append("import jakarta.ws.rs.core.MediaType;\n\n");
        
        code.append("/**\n");
        code.append(" * REST endpoint to expose the Agent Card\n");
        code.append(" */\n");
        code.append("@Path(\"/\")\n");
        code.append("public class ").append(className).append(" {\n\n");
        
        code.append("    @Inject\n");
        code.append("    @PublicAgentCard\n");
        code.append("    AgentCard agentCard;\n\n");
        
        code.append("    @GET\n");
        code.append("    @Path(\".well-known/agent-card\")\n");
        code.append("    @Produces(MediaType.APPLICATION_JSON)\n");
        code.append("    public AgentCard getAgentCardWellKnown() {\n");
        code.append("        return agentCard;\n");
        code.append("    }\n\n");
        
        code.append("    @GET\n");
        code.append("    @Path(\"agent-card\")\n");
        code.append("    @Produces(MediaType.APPLICATION_JSON)\n");
        code.append("    public AgentCard getAgentCard() {\n");
        code.append("        return agentCard;\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    private String generateApplicationProperties(GenerationRequest request) {
        int port = request.serverPort != null ? request.serverPort : 8000;
        return "# Quarkus Configuration\n" +
               "quarkus.http.port=" + port + "\n" +
               "quarkus.http.host=0.0.0.0\n\n" +
               "# Application\n" +
               "quarkus.application.name=" + request.agentName.toLowerCase().replace(" ", "-") + "-agent\n\n" +
               "# Logging\n" +
               "quarkus.log.console.enable=true\n" +
               "quarkus.log.console.level=INFO\n" +
               "quarkus.log.category.\"" + request.packageName + "\".level=DEBUG\n\n" +
               "# A2A Server Configuration\n" +
               "# Add any A2A-specific configuration here\n";
    }
    
    private String generateStartScript(GenerationRequest request) {
        int port = request.serverPort != null ? request.serverPort : 8000;
        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n");
        script.append("# Start script for ").append(capitalize(request.agentName)).append("Agent A2A Server\n\n");
        script.append("echo \"Starting ").append(capitalize(request.agentName)).append(" A2A Server...\"\n\n");
        
        // If API key was provided during generation, set it
        if (request.googleApiKey != null && !request.googleApiKey.trim().isEmpty()) {
            script.append("# Setting Google API Key from generation\n");
            script.append("export GOOGLE_API_KEY=\"").append(request.googleApiKey).append("\"\n\n");
        }
        
        script.append("# Check if GOOGLE_API_KEY is set\n");
        script.append("if [ -z \"$GOOGLE_API_KEY\" ]; then\n");
        script.append("    echo \"Error: GOOGLE_API_KEY environment variable is not set\"\n");
        script.append("    echo \"Get your API key from: https://aistudio.google.com/app/apikey\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");
        script.append("# Configured port (can be overridden with first argument)\n");
        script.append("PORT=${1:-").append(port).append("}\n\n");
        script.append("echo \"Starting server on port $PORT...\"\n");
        script.append("mvn quarkus:dev -Dquarkus.http.port=$PORT\n");
        
        return script.toString();
    }
    
    private String generateStartScriptBat(GenerationRequest request) {
        int port = request.serverPort != null ? request.serverPort : 8000;
        StringBuilder script = new StringBuilder();
        script.append("@echo off\r\n");
        script.append("REM Start script for ").append(capitalize(request.agentName)).append("Agent A2A Server\r\n\r\n");
        script.append("echo Starting ").append(capitalize(request.agentName)).append(" A2A Server...\r\n\r\n");
        
        // If API key was provided during generation, set it
        if (request.googleApiKey != null && !request.googleApiKey.trim().isEmpty()) {
            script.append("REM Setting Google API Key from generation\r\n");
            script.append("set GOOGLE_API_KEY=").append(request.googleApiKey).append("\r\n\r\n");
        }
        
        script.append("REM Check if GOOGLE_API_KEY is set\r\n");
        script.append("if not defined GOOGLE_API_KEY (\r\n");
        script.append("    echo Error: GOOGLE_API_KEY environment variable is not set\r\n");
        script.append("    echo Get your API key from: https://aistudio.google.com/app/apikey\r\n");
        script.append("    exit /b 1\r\n");
        script.append(")\r\n\r\n");
        script.append("REM Configured port (can be overridden with first argument)\r\n");
        script.append("if \"%1\"==\"\" (\r\n");
        script.append("    set PORT=").append(port).append("\r\n");
        script.append(") else (\r\n");
        script.append("    set PORT=%1\r\n");
        script.append(")\r\n\r\n");
        script.append("echo Starting server on port %PORT%...\r\n");
        script.append("mvn quarkus:dev -Dquarkus.http.port=%PORT%\r\n");
        
        return script.toString();
    }
}
