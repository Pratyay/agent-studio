# Agent Studio

A visual agent builder that generates AI agents with **MCP (Model Context Protocol)** tools and **A2A (Agent-to-Agent)** sub-agents using Google ADK and Quarkus.

## Overview

Agent Studio is a web-based tool that enables you to:

- 🛠️ **Register and manage MCP tool servers** - Connect external tools and services via MCP
- 🤖 **Register A2A agents as sub-agents** - Build hierarchical agent architectures
- 🎨 **Visual drag-and-drop builder** - Compose agents by dragging tools and sub-agents
- 🚀 **Generate production-ready code** - Download complete Maven projects with all dependencies
- 💬 **Test agents in real-time** - Chat with your agents directly in the UI

## Architecture

### Generated Agents

Each generated agent is a complete Quarkus application that includes:

- **Google ADK (Agent Development Kit)** for LLM agent orchestration
- **MCP Java SDK** for tool integration
- **A2A Java SDK** for agent-to-agent communication
- **Quarkus** for server runtime and dependency injection

### Key Features

#### 1. MCP Tool Integration
- Connect to any MCP-compliant tool server
- Agents automatically discover and use tool capabilities
- Supports SSE (Server-Sent Events) transport

#### 2. A2A Sub-Agent Delegation
- Supervisor agents can delegate tasks to specialized sub-agents
- Sub-agents expose their capabilities via AgentCards
- Supports hierarchical agent architectures (agents calling agents)

#### 3. Configurable Deployment
- Customizable server ports
- Environment-based configuration (Google API keys)
- Production-ready startup scripts

## Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **Google AI API Key** - Get one from [Google AI Studio](https://aistudio.google.com/app/apikey)

### Running Agent Studio

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd agent-studio
   ```

2. **Build the project:**
   ```bash
   mvn clean package
   ```

3. **Run the application:**
   ```bash
   mvn quarkus:dev
   ```

4. **Open in browser:**
   ```
   http://localhost:8080
   ```

## Usage Guide

### 1. Register MCP Tools

Click **"➕ Register MCP Tool"** to add external tools:

- **Tool Name:** Identifier for the tool (e.g., `weather-tool`)
- **Description:** What the tool does
- **MCP Server Endpoint:** URL where the MCP server is running (e.g., `http://localhost:3000`)

### 2. Register A2A Agents

Click **"🤖 Register A2A Agent"** to add sub-agents:

- **A2A Agent URL:** URL of the agent's AgentCard (e.g., `http://localhost:8001`)
- Click **"🔍 Test Connection"** to verify the agent is reachable
- Review the agent's capabilities before registering

### 3. Build Your Agent

1. Switch between **"Attached Tools"** and **"Sub-Agents"** tabs
2. **Drag and drop** tools and agents from the left sidebar to the canvas
3. Configure agent properties on the right:
   - **Agent Name:** Identifier for your agent
   - **Description:** What your agent does
   - **Instructions:** System prompt / behavior guidelines
   - **Package Name:** Java package for generated code
   - **Google API Key:** Your Gemini API key
   - **Server Port:** Port for the agent server (default: 8000)

### 4. Generate Agent Code

Click **"🚀 Generate & Download Agent"** to get a ZIP file containing:

```
your-agent/
├── pom.xml                          # Maven project file
├── README.md                        # Agent-specific documentation
├── config.yml                       # Tool configurations
├── start-server.sh                  # Startup script (Unix)
├── start-server.bat                 # Startup script (Windows)
└── src/
    └── main/
        ├── java/
        │   └── com/example/agent/
        │       ├── YourAgent.java              # Main agent class
        │       ├── YourAgentCardProducer.java  # A2A AgentCard
        │       └── YourAgentExecutorProducer.java # A2A Executor
        └── resources/
            └── application.properties          # Quarkus config
```

### 5. Run Your Generated Agent

1. **Extract the ZIP file**
2. **Set your Google API key:**
   ```bash
   export GOOGLE_API_KEY=your-api-key-here
   ```
3. **Run the agent:**
   ```bash
   ./start-server.sh
   # or
   mvn quarkus:dev
   ```
4. **Access your agent:**
   - AgentCard: `http://localhost:{port}/agent-card`
   - A2A tasks: `http://localhost:{port}/tasks`

## Example Use Cases

### 1. Travel Agent with Weather Tool

Create an agent that:
- Accepts MCP tool: `weather-tool` (provides current weather)
- Uses Gemini to plan trips based on weather conditions

### 2. Supervisor Agent with Specialized Sub-Agents

Create a supervisor that delegates to:
- **Research Agent** - Gathers information
- **Analysis Agent** - Analyzes data
- **Writing Agent** - Produces final output

The supervisor orchestrates these sub-agents to complete complex tasks.

## Project Structure

```
agent-studio/
├── src/main/java/com/example/agent/
│   └── registry/
│       ├── AgentCodeGenerator.java       # Generates agent code
│       ├── AgentGeneratorResource.java   # REST API for generation
│       ├── ToolRegistry.java             # Manages MCP tools
│       ├── A2AClientService.java         # A2A agent registry
│       └── MCPClient.java                # MCP server connector
└── src/main/resources/
    └── static/
        └── unified-builder.html          # Web UI
```

## Technologies Used

- **[Google ADK](https://github.com/google/adk)** - Agent orchestration framework
- **[MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)** - Model Context Protocol implementation
- **[A2A Java SDK](https://github.com/a2asdk/a2a-java-sdk)** - Agent-to-Agent protocol implementation
- **[Quarkus](https://quarkus.io/)** - Cloud-native Java framework
- **[Gemini](https://ai.google.dev/)** - Google's generative AI model

## Architecture Patterns

### MCP Tool Integration

```
Agent ──► McpToolset ──► MCP Server ──► External Tool
```

The agent uses Google ADK's `McpToolset` to connect to MCP servers, which expose tool capabilities that the LLM can invoke.

### A2A Agent Delegation

```
Supervisor Agent ──► A2A Client ──► Sub-Agent ──► Response
        │                                  │
        └─────────────────────────────────┘
              (receives response)
```

The supervisor agent uses the A2A SDK to communicate with sub-agents via their AgentCard endpoints.

## Configuration

### Environment Variables

- `GOOGLE_API_KEY` - Required for Gemini model access
- `quarkus.http.port` - Server port (default: 8000)

### Generated Agent Configuration

Each generated agent includes:
- `application.properties` - Quarkus configuration
- `config.yml` - Tool endpoint configurations

## Troubleshooting

### Agent doesn't respond

- Check that `GOOGLE_API_KEY` is set
- Verify MCP servers are running and accessible
- Check agent logs for error messages

### Sub-agent timeout

- Ensure sub-agent is running and accessible
- Check network connectivity between agents
- Verify AgentCard URL is correct

### Tool not found

- Verify MCP server endpoint is correct
- Test MCP server connectivity manually
- Check MCP server logs

## Contributing

Contributions are welcome! Please ensure:
- Code follows existing patterns
- All changes are tested
- Commit messages are descriptive

## License

[Specify your license here]

## Support

For issues and questions, please open a GitHub issue or contact the maintainers.
