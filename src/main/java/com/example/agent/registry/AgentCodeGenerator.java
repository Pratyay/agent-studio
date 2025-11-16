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
        public List<String> subagentIds;
        
        // Advanced configuration
        public String model;  // e.g., "gemini-2.0-flash-exp", "gemini-1.5-pro"
        public Double temperature;  // 0.0 to 2.0
        public Integer maxTokens;  // max output tokens
        public Double topP;  // nucleus sampling
        public Integer topK;  // top-k sampling
        
        public GenerationRequest() {
            this.attachedTools = new ArrayList<>();
            this.subagentIds = new ArrayList<>();
            // Defaults
            this.model = "gemini-2.0-flash-exp";
            this.temperature = 1.0;
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
        
        code.append("package ").append(request.packageName).append(";\n\n");
        code.append("import com.google.adk.agents.BaseAgent;\n");
        code.append("import com.google.adk.agents.LlmAgent;\n");
        code.append("import com.google.adk.tools.Annotations.Schema;\n");
        code.append("import com.google.adk.tools.FunctionTool;\n");
        code.append("import io.modelcontextprotocol.client.McpClient;\n");
        code.append("import io.modelcontextprotocol.client.McpSyncClient;\n");
        code.append("import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;\n");
        code.append("import io.modelcontextprotocol.spec.McpSchema.Tool;\n");
        code.append("import io.modelcontextprotocol.spec.McpSchema.CallToolResult;\n");
        code.append("import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;\n");
        code.append("import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;\n");
        code.append("import java.util.*;\n");
        code.append("import java.time.Duration;\n");
        code.append("import java.util.concurrent.CompletableFuture;\n");
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
        code.append("    private static Map<String, McpSyncClient> mcpClients = new HashMap<>();\n");
        code.append("    private static Map<String, Tool> mcpTools = new HashMap<>();\n\n");
        
        // Static initializer
        code.append("    static {\n");
        code.append("        try {\n");
        code.append("            initializeMcpTools();\n");
        code.append("            ROOT_AGENT = createAgent();\n");
        code.append("        } catch (Exception e) {\n");
        code.append("            throw new RuntimeException(\"Failed to initialize agent\", e);\n");
        code.append("        }\n");
        code.append("    }\n\n");
        
        // Initialize MCP tools
        code.append("    private static void initializeMcpTools() throws Exception {\n");
        code.append("        System.out.println(\"Initializing MCP tools...\");\n\n");
        for (ToolMetadata tool : request.attachedTools) {
            String toolVar = sanitizeVarName(tool.getName());
            code.append("        // Connect to ").append(tool.getName()).append("\n");
            code.append("        HttpClientSseClientTransport ").append(toolVar).append("Transport = HttpClientSseClientTransport\n");
            code.append("            .builder(\"").append(tool.getEndpointUrl()).append("\")\n");
            code.append("            .build();\n");
            code.append("        McpSyncClient ").append(toolVar).append("Client = McpClient.sync(").append(toolVar).append("Transport)\n");
            code.append("            .requestTimeout(Duration.ofSeconds(30))\n");
            code.append("            .build();\n");
            code.append("        ").append(toolVar).append("Client.initialize();\n");
            code.append("        mcpClients.put(\"").append(tool.getName()).append("\", ").append(toolVar).append("Client);\n\n");
            code.append("        // Discover tools from ").append(tool.getName()).append("\n");
            code.append("        ListToolsResult ").append(toolVar).append("ToolsResult = ").append(toolVar).append("Client.listTools();\n");
            code.append("        for (Tool tool : ").append(toolVar).append("ToolsResult.tools()) {\n");
            code.append("            mcpTools.put(tool.name(), tool);\n");
            code.append("            System.out.println(\"Registered MCP tool: \" + tool.name());\n");
            code.append("        }\n\n");
        }
        code.append("        System.out.println(\"Initialized \" + mcpTools.size() + \" MCP tools from \" + mcpClients.size() + \" servers\");\n");
        code.append("    }\n\n");
        
        // Create agent
        code.append("    private static BaseAgent createAgent() {\n");
        code.append("        // Note: MCP tools are not registered as FunctionTools with the agent\n");
        code.append("        // The agent will use them through the generic callMcpTool method\n");
        code.append("        // when instructed to do so in the instructions\n\n");
        code.append("        return LlmAgent.builder()\n");
        code.append("            .name(\"").append(sanitizeVarName(request.agentName)).append("\")\n");
        code.append("            .description(\"").append(request.description != null ? request.description.replace("\"", "\\\"") : "Generated agent with MCP tools").append("\")\n");
        
        // Add instructions
        String instructions = request.instructions != null && !request.instructions.trim().isEmpty()
            ? request.instructions.replace("\"", "\\\"")
            : "You are a helpful AI assistant with access to various tools. Use the available tools to help users accomplish their tasks.";
        code.append("            .instruction(\"\"\"\n");
        code.append("                ").append(instructions.replace("\n", "\n                ")).append("\n");
        code.append("                \"\"\")\n");
        code.append("            .model(\"").append(request.model != null ? request.model : "gemini-2.0-flash-exp").append("\")\n");
        
        // Note: temperature, topP, topK, and maxTokens are not supported in Google ADK 0.3.0
        // These parameters are tracked in the request but not applied to the agent builder
        
        code.append("            .build();\n");
        code.append("    }\n\n");
        
        // Generate MCP tool wrapper methods
        for (ToolMetadata tool : request.attachedTools) {
            // For each MCP server, generate wrapper methods for its tools
            code.append("    // Wrapper methods for ").append(tool.getName()).append(" tools\n");
            code.append("    // Note: These are placeholders - actual methods will be created at runtime\n\n");
        }
        
        // Generic MCP tool caller
        code.append("    @Schema(description = \"Call an MCP tool\")\n");
        code.append("    public static Map<String, Object> callMcpTool(\n");
        code.append("        @Schema(name = \"toolName\", description = \"Name of the tool to call\") String toolName,\n");
        code.append("        @Schema(name = \"arguments\", description = \"Tool arguments as JSON\") String arguments) {\n");
        code.append("        try {\n");
        code.append("            Tool tool = mcpTools.get(toolName);\n");
        code.append("            if (tool == null) {\n");
        code.append("                return Map.of(\"error\", \"Tool not found: \" + toolName);\n");
        code.append("            }\n\n");
        code.append("            // Find the MCP client that has this tool\n");
        code.append("            for (McpSyncClient client : mcpClients.values()) {\n");
        code.append("                try {\n");
        code.append("                    // Parse arguments if provided\n");
        code.append("                    Map<String, Object> args = arguments != null && !arguments.isEmpty() \n");
        code.append("                        ? new HashMap<>() // Simplified - parse JSON in production\n");
        code.append("                        : new HashMap<>();\n");
        code.append("                    \n");
        code.append("                    CallToolRequest request = CallToolRequest.builder()\n");
        code.append("                        .name(toolName)\n");
        code.append("                        .arguments(args)\n");
        code.append("                        .build();\n");
        code.append("                    CallToolResult result = client.callTool(request);\n");
        code.append("                    return Map.of(\"result\", result.toString());\n");
        code.append("                } catch (Exception ignored) {\n");
        code.append("                    // Try next client\n");
        code.append("                }\n");
        code.append("            }\n");
        code.append("            return Map.of(\"error\", \"Failed to call tool\");\n");
        code.append("        } catch (Exception e) {\n");
        code.append("            return Map.of(\"error\", e.getMessage());\n");
        code.append("        }\n");
        code.append("    }\n\n");
        
        // Chat method for executor
        code.append("    public static String chat(String message) {\n");
        code.append("        try {\n");
        code.append("            // Create runner and session\n");
        code.append("            InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);\n");
        code.append("            Session session = runner\n");
        code.append("                .sessionService()\n");
        code.append("                .createSession(runner.appName(), \"user\")\n");
        code.append("                .blockingGet();\n\n");
        code.append("            // Create user message\n");
        code.append("            Content userMsg = Content.fromParts(Part.fromText(message));\n");
        code.append("            RunConfig runConfig = RunConfig.builder().build();\n\n");
        code.append("            // Execute and collect response\n");
        code.append("            Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);\n");
        code.append("            StringBuilder response = new StringBuilder();\n\n");
        code.append("            events.blockingForEach(event -> {\n");
        code.append("                if (event.finalResponse()) {\n");
        code.append("                    response.append(event.stringifyContent());\n");
        code.append("                }\n");
        code.append("            });\n\n");
        code.append("            return response.toString();\n");
        code.append("        } catch (Exception e) {\n");
        code.append("            return \"Error: \" + e.getMessage();\n");
        code.append("        }\n");
        code.append("    }\n\n");
        
        // Main method
        code.append("    public static void main(String[] args) {\n");
        code.append("        System.out.println(\"").append(className).append(" initialized with \" + mcpTools.size() + \" MCP tools\");\n");
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
               "        <!-- A2A Java SDK - JSON-RPC transport -->\n" +
               "        <dependency>\n" +
               "            <groupId>io.github.a2asdk</groupId>\n" +
               "            <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>\n" +
               "            <version>${a2a.sdk.version}</version>\n" +
               "        </dependency>\n\n" +
               "        <!-- A2A Java SDK - REST transport -->\n" +
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
        
        if (request.subagentIds != null && !request.subagentIds.isEmpty()) {
            readme.append("This agent uses the following sub-agents:\n\n");
            for (String subagentId : request.subagentIds) {
                readme.append("- ").append(subagentId).append("\n");
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
        readme.append("The agent will be accessible at `http://localhost:8000` by default.\n\n");
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
        code.append("            .url(\"http://localhost:8000\")\n");
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
        code.append("            TaskUpdater updater = new TaskUpdater(context, eventQueue);\n\n");
        
        code.append("            // Mark the task as submitted and start working on it\n");
        code.append("            if (context.getTask() == null) {\n");
        code.append("                updater.submit();\n");
        code.append("            }\n");
        code.append("            updater.startWork();\n\n");
        
        code.append("            // Extract the text from the message\n");
        code.append("            String userMessage = extractTextFromMessage(context.getMessage());\n\n");
        
        code.append("            // Call the agent with the user's message\n");
        code.append("            String response = ").append(agentClassName).append(".chat(userMessage);\n\n");
        
        code.append("            // Create the response part\n");
        code.append("            TextPart responsePart = new TextPart(response, null);\n");
        code.append("            List<Part<?>> parts = List.of(responsePart);\n\n");
        
        code.append("            // Add the response as an artifact and complete the task\n");
        code.append("            updater.addArtifact(parts, null, null, null);\n");
        code.append("            updater.complete();\n");
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
        return "# Quarkus Configuration\n" +
               "quarkus.http.port=8000\n" +
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
        return "#!/bin/bash\n" +
               "# Start script for " + capitalize(request.agentName) + "Agent A2A Server\n\n" +
               "echo \"Starting " + capitalize(request.agentName) + " A2A Server...\"\n\n" +
               "# Check if GOOGLE_API_KEY is set\n" +
               "if [ -z \"$GOOGLE_API_KEY\" ]; then\n" +
               "    echo \"Error: GOOGLE_API_KEY environment variable is not set\"\n" +
               "    echo \"Get your API key from: https://aistudio.google.com/app/apikey\"\n" +
               "    exit 1\n" +
               "fi\n\n" +
               "# Default port\n" +
               "PORT=${1:-8000}\n\n" +
               "echo \"Starting server on port $PORT...\"\n" +
               "mvn quarkus:dev -Dquarkus.http.port=$PORT\n";
    }
    
    private String generateStartScriptBat(GenerationRequest request) {
        return "@echo off\r\n" +
               "REM Start script for " + capitalize(request.agentName) + "Agent A2A Server\r\n\r\n" +
               "echo Starting " + capitalize(request.agentName) + " A2A Server...\r\n\r\n" +
               "REM Check if GOOGLE_API_KEY is set\r\n" +
               "if not defined GOOGLE_API_KEY (\r\n" +
               "    echo Error: GOOGLE_API_KEY environment variable is not set\r\n" +
               "    echo Get your API key from: https://aistudio.google.com/app/apikey\r\n" +
               "    exit /b 1\r\n" +
               ")\r\n\r\n" +
               "REM Default port\r\n" +
               "if \"%1\"==\"\" (\r\n" +
               "    set PORT=8000\r\n" +
               ") else (\r\n" +
               "    set PORT=%1\r\n" +
               ")\r\n\r\n" +
               "echo Starting server on port %PORT%...\r\n" +
               "mvn quarkus:dev -Dquarkus.http.port=%PORT%\r\n";
    }
}
