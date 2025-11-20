// Technical Documentation Content for Agent Studio
const DOCS_CONTENT = `
<div class="form-content">
    <div class="form-header">
        <h1>Technical Documentation</h1>
        <p>Architecture, requirements, and implementation details</p>
    </div>

    <!-- Project Objective -->
    <div class="section">
        <h2 style="font-size: 22px; font-weight: 600; margin-bottom: 16px; color: #050505; border-bottom: 2px solid #1877f2; padding-bottom: 8px;">Project Objective</h2>
        
        <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 24px; border-radius: 12px; margin-bottom: 24px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);">
            <p style="font-size: 16px; line-height: 1.8; color: #ffffff; margin: 0; font-weight: 500;">
                Agent Studio is a comprehensive code generation platform designed to accelerate the development of production-ready AI agents. 
                It enables developers to visually compose, configure, and deploy intelligent agents that integrate seamlessly with external tools 
                via the Model Context Protocol (MCP) and communicate with other agents through the Agent-to-Agent (A2A) protocol.
            </p>
        </div>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Core Goals</h3>
        
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px;">
            <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; border-left: 4px solid #1877f2;">
                <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">Simplify Agent Development</h4>
                <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin: 0;">
                    Eliminate boilerplate code and complex configuration by generating complete, runnable agent projects with minimal setup.
                </p>
            </div>
            
            <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; border-left: 4px solid #42b883;">
                <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">Enable Tool Integration</h4>
                <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin: 0;">
                    Provide seamless integration with MCP-compliant tool servers, allowing agents to interact with external systems and APIs.
                </p>
            </div>
            
            <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; border-left: 4px solid #e67e22;">
                <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">Support Multi-Agent Systems</h4>
                <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin: 0;">
                    Enable hierarchical agent architectures where supervisor agents orchestrate specialized sub-agents for complex task decomposition.
                </p>
            </div>
            
            <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; border-left: 4px solid #9b59b6;">
                <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">Accelerate Time-to-Production</h4>
                <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin: 0;">
                    Generate deployment-ready code with proper dependency management, configuration files, and startup scripts for immediate use.
                </p>
            </div>
        </div>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Target Users</h3>
        <ul style="padding-left: 24px; color: #65676b; font-size: 15px; line-height: 1.8;">
            <li><strong>AI Application Developers:</strong> Building intelligent agents powered by large language models</li>
            <li><strong>System Integrators:</strong> Connecting AI agents with enterprise tools and services</li>
            <li><strong>Research Teams:</strong> Prototyping multi-agent systems and evaluating agent architectures</li>
            <li><strong>DevOps Engineers:</strong> Deploying scalable agent-based solutions in production environments</li>
        </ul>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Value Proposition</h3>
        <div style="background: #fff3cd; padding: 16px; border-radius: 8px; border-left: 4px solid #ffc107; margin-bottom: 20px;">
            <p style="font-size: 15px; line-height: 1.7; color: #856404; margin: 0;">
                <strong>Reduce development time from weeks to minutes.</strong> Agent Studio automates the scaffolding, configuration, and integration work, 
                allowing developers to focus on defining agent behavior and business logic rather than infrastructure setup.
            </p>
        </div>
    </div>

    <!-- Functional Requirements -->
    <div class="section">
        <h2 style="font-size: 22px; font-weight: 600; margin-bottom: 16px; color: #050505; border-bottom: 2px solid #1877f2; padding-bottom: 8px;">Functional Requirements</h2>
        
        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">FR1: Agent Configuration and Management</h3>
        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #1877f2;">
            <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin-bottom: 12px;">
                The system shall provide a web-based interface for configuring AI agents with the following capabilities:
            </p>
            <ul style="padding-left: 24px; color: #65676b; font-size: 14px; line-height: 1.8;">
                <li>Define agent name, description, and system instructions</li>
                <li>Specify Java package name and server port configuration</li>
                <li>Configure Google Gemini API credentials</li>
                <li>Validate all required fields before code generation</li>
            </ul>
        </div>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">FR2: MCP Tool Integration</h3>
        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #1877f2;">
            <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin-bottom: 12px;">
                The system shall support registration and integration of Model Context Protocol (MCP) tools:
            </p>
            <ul style="padding-left: 24px; color: #65676b; font-size: 14px; line-height: 1.8;">
                <li>Register MCP tool servers with endpoint URLs</li>
                <li>Validate server connectivity and health status</li>
                <li>Query MCP servers for available tool capabilities via SSE transport</li>
                <li>Display tool metadata including name, description, and available functions</li>
                <li>Enable drag-and-drop attachment of tools to agents</li>
                <li>Store tool configurations persistently in Redis</li>
            </ul>
        </div>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">FR3: Agent-to-Agent (A2A) Communication</h3>
        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #1877f2;">
            <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin-bottom: 12px;">
                The system shall enable hierarchical multi-agent architectures through A2A protocol:
            </p>
            <ul style="padding-left: 24px; color: #65676b; font-size: 14px; line-height: 1.8;">
                <li>Register sub-agents via AgentCard URL discovery</li>
                <li>Test connectivity to registered sub-agents</li>
                <li>Display sub-agent capabilities and supported protocols</li>
                <li>Support multiple transport protocols (REST, JSON-RPC, gRPC)</li>
                <li>Enable supervisor agents to delegate tasks to specialized sub-agents</li>
                <li>Handle task serialization and response deserialization</li>
            </ul>
        </div>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">FR4: Code Generation</h3>
        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #1877f2;">
            <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin-bottom: 12px;">
                The system shall generate production-ready agent code with complete project structure:
            </p>
            <ul style="padding-left: 24px; color: #65676b; font-size: 14px; line-height: 1.8;">
                <li>Generate Maven POM file with all required dependencies</li>
                <li>Create Java agent class extending Google ADK Agent framework</li>
                <li>Inject configured MCP tools and A2A sub-agents into agent code</li>
                <li>Generate A2A AgentCard and Executor producer classes</li>
                <li>Create configuration files (application.properties, config.yml)</li>
                <li>Include startup scripts for Unix and Windows platforms</li>
                <li>Generate comprehensive README with setup instructions</li>
                <li>Package all files as downloadable ZIP archive</li>
            </ul>
        </div>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">FR5: Callback System</h3>
        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #1877f2;">
            <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin-bottom: 12px;">
                The system shall provide extensible callback hooks for agent lifecycle management:
            </p>
            <ul style="padding-left: 24px; color: #65676b; font-size: 14px; line-height: 1.8;">
                <li>Support before-agent and after-agent callback registration</li>
                <li>Enable stateless lambda-based callbacks for simple operations</li>
                <li>Support stateful CDI bean callbacks for complex behaviors</li>
                <li>Allow custom callback definitions with configurable scopes</li>
                <li>Integrate callbacks into generated agent code</li>
            </ul>
        </div>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">FR6: Registry Management</h3>
        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #1877f2;">
            <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin-bottom: 12px;">
                The system shall maintain persistent registries for tools and agents:
            </p>
            <ul style="padding-left: 24px; color: #65676b; font-size: 14px; line-height: 1.8;">
                <li>Store tool and agent metadata in Redis backend</li>
                <li>Support CRUD operations (create, read, update, delete)</li>
                <li>Publish change notifications via Redis pub/sub</li>
                <li>Enable discovery of agents by capability attributes</li>
                <li>Track registration timestamps and health status</li>
            </ul>
        </div>
    </div>

    <!-- Design Tenets -->
    <div class="section">
        <h2 style="font-size: 22px; font-weight: 600; margin-bottom: 16px; color: #050505; border-bottom: 2px solid #1877f2; padding-bottom: 8px;">Design Tenets</h2>
        
        <div style="margin-bottom: 20px;">
            <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">1. Modularity and Extensibility</h3>
            <p style="font-size: 15px; line-height: 1.7; color: #65676b; margin-bottom: 12px;">
                The system is designed with clear separation of concerns. Each component (Tool Registry, Agent Registry, Code Generator) operates independently with well-defined interfaces. This enables easy extension without modifying core logic.
            </p>
        </div>

        <div style="margin-bottom: 20px;">
            <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">2. Protocol-Agnostic Communication</h3>
            <p style="font-size: 15px; line-height: 1.7; color: #65676b; margin-bottom: 12px;">
                Support for multiple transport protocols (REST, JSON-RPC, gRPC) allows agents to communicate using the most appropriate protocol for their use case. The A2A SDK abstracts transport details from business logic.
            </p>
        </div>

        <div style="margin-bottom: 20px;">
            <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">3. Dynamic Discovery and Registration</h3>
            <p style="font-size: 15px; line-height: 1.7; color: #65676b; margin-bottom: 12px;">
                Tools and agents register themselves at runtime. The system queries MCP servers for capabilities rather than storing static configurations, ensuring up-to-date tool information.
            </p>
        </div>

        <div style="margin-bottom: 20px;">
            <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">4. Code Generation Over Configuration</h3>
            <p style="font-size: 15px; line-height: 1.7; color: #65676b; margin-bottom: 12px;">
                Generated agents are complete, standalone Maven projects with no dependency on Agent Studio at runtime. This ensures portability and allows teams to customize generated code without constraints.
            </p>
        </div>

        <div style="margin-bottom: 20px;">
            <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">5. Production-Ready by Default</h3>
            <p style="font-size: 15px; line-height: 1.7; color: #65676b; margin-bottom: 12px;">
                Generated agents include proper dependency injection, configuration management, health checks, and deployment scripts. No additional scaffolding is required to run agents in production environments.
            </p>
        </div>
    </div>

    <!-- System Architecture -->
    <div class="section">
        <h2 style="font-size: 22px; font-weight: 600; margin-bottom: 16px; color: #050505; border-bottom: 2px solid #1877f2; padding-bottom: 8px;">System Architecture</h2>
        
        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">High-Level Architecture</h3>
        <p style="font-size: 15px; line-height: 1.7; color: #65676b; margin-bottom: 16px;">
            Agent Studio follows a three-tier architecture consisting of the presentation layer (web UI), application layer (REST API and registries), and integration layer (MCP/A2A clients).
        </p>
       
    <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 20px; text-align: center;">
            <img src="/ui/img/agent-registry-hld.jpg" alt="Agent Studio Architecture Diagram" style="max-width: 100%; border: 1px solid #e4e6eb; border-radius: 4px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
        </div>
        
        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Core Components</h3>
        
        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Low-Level Design</h3>
        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 20px; text-align: center;">
            <img src="/ui/img/agent-registry-lld.jpg" alt="Agent Studio Architecture Diagram" style="max-width: 100%; border: 1px solid #e4e6eb; border-radius: 4px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
        </div>
        
        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #1877f2;">
            <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">Tool Registry</h4>
            <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin-bottom: 8px;">
                Manages MCP tool server registrations with Redis persistence. Performs health checks and dynamically enriches tool metadata by querying MCP servers for available capabilities.
            </p>
            <code style="font-size: 13px; font-family: monospace; color: #1877f2;">ToolRegistry.java, MCPClient.java</code>
        </div>

        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #1877f2;">
            <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">Agent Registry</h4>
            <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin-bottom: 8px;">
                Central registry for managing agent metadata with pub/sub notifications. Supports agent discovery by capabilities and provides lifecycle management (registration, updates, unregistration).
            </p>
            <code style="font-size: 13px; font-family: monospace; color: #1877f2;">AgentRegistry.java, A2AClientService.java</code>
        </div>

        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #1877f2;">
            <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">Agent Code Generator</h4>
            <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin-bottom: 8px;">
                Generates complete Maven projects with agent implementation, configuration files, and startup scripts. Handles dependency resolution and injects MCP tools and A2A sub-agents into generated code.
            </p>
            <code style="font-size: 13px; font-family: monospace; color: #1877f2;">AgentCodeGenerator.java, AgentGeneratorResource.java</code>
        </div>

        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #1877f2;">
            <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">Callback Registry</h4>
            <p style="font-size: 14px; line-height: 1.6; color: #65676b; margin-bottom: 8px;">
                Manages extensible callback hooks (before/after agent execution). Supports both stateless lambda-based callbacks and stateful CDI bean callbacks for complex agent behaviors.
            </p>
            <code style="font-size: 13px; font-family: monospace; color: #1877f2;">CallbackRegistry.java, CallbackRegistryResource.java</code>
        </div>
    </div>

    <!-- Data Flow -->
    <div class="section">
        <h2 style="font-size: 22px; font-weight: 600; margin-bottom: 16px; color: #050505; border-bottom: 2px solid #1877f2; padding-bottom: 8px;">Data Flow</h2>
        
        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Agent Generation Flow</h3>
        <ol style="padding-left: 24px; color: #65676b; font-size: 15px; line-height: 1.8; margin-bottom: 20px;">
            <li>User configures agent via web UI (name, instructions, tools, sub-agents)</li>
            <li>UI sends POST request to <code>/api/agents/generate</code></li>
            <li>AgentGeneratorResource fetches tool metadata from ToolRegistry</li>
            <li>AgentGeneratorResource fetches sub-agent metadata from A2AClientService</li>
            <li>AgentCodeGenerator creates Maven project structure in memory</li>
            <li>Generator templates Java source files with injected dependencies</li>
            <li>Project is packaged as ZIP archive and returned to client</li>
            <li>User extracts ZIP and runs agent with <code>mvn quarkus:dev</code></li>
        </ol>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">MCP Tool Integration Flow</h3>
        <ol style="padding-left: 24px; color: #65676b; font-size: 15px; line-height: 1.8; margin-bottom: 20px;">
            <li>User registers MCP server endpoint via UI</li>
            <li>ToolRegistry validates server connectivity using MCPClient</li>
            <li>Registry queries MCP server for available tools via SSE transport</li>
            <li>Tool capabilities are stored in Redis with enriched metadata</li>
            <li>Generated agents use McpToolset to connect to MCP servers</li>
            <li>At runtime, agent queries MCP server and invokes tools dynamically</li>
        </ol>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">A2A Communication Flow</h3>
        <ol style="padding-left: 24px; color: #65676b; font-size: 15px; line-height: 1.8; margin-bottom: 20px;">
            <li>Supervisor agent receives task from user or upstream agent</li>
            <li>Agent analyzes task and determines required sub-agent capabilities</li>
            <li>A2A client creates task payload with instructions and context</li>
            <li>Client selects transport protocol (REST/JSON-RPC/gRPC)</li>
            <li>Task is dispatched to sub-agent's task endpoint</li>
            <li>Sub-agent processes task using its specialized tools and LLM</li>
            <li>Response is returned to supervisor agent for integration</li>
            <li>Supervisor synthesizes final response from sub-agent outputs</li>
        </ol>
    </div>

    <!-- Generated Agent Architecture -->
    <div class="section">
        <h2 style="font-size: 22px; font-weight: 600; margin-bottom: 16px; color: #050505; border-bottom: 2px solid #1877f2; padding-bottom: 8px;">Generated Agent Architecture</h2>
        
        <p style="font-size: 15px; line-height: 1.7; color: #65676b; margin-bottom: 16px;">
            Each generated agent is a self-contained Quarkus application with the following structure:
        </p>

        <div style="background: #1e1e1e; color: #d4d4d4; padding: 16px; border-radius: 8px; font-family: 'SF Mono', monospace; font-size: 13px; margin-bottom: 20px; overflow-x: auto;">
<pre style="margin: 0;">generated-agent/
├── pom.xml                                 # Maven dependencies
├── README.md                               # Agent-specific documentation
├── config.yml                              # MCP tool endpoints
├── start-server.sh                         # Unix startup script
├── start-server.bat                        # Windows startup script
└── src/main/
    ├── java/com/example/agent/
    │   ├── YourAgent.java                  # ADK agent implementation
    │   ├── YourAgentCardProducer.java      # A2A AgentCard provider
    │   └── YourAgentExecutorProducer.java  # A2A task executor
    └── resources/
        └── application.properties           # Quarkus configuration</pre>
        </div>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Key Components</h3>
        
        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px;">
            <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">Agent Class</h4>
            <p style="font-size: 14px; line-height: 1.6; color: #65676b;">
                Extends <code>Agent</code> from Google ADK. Configures LLM (Gemini), system instructions, MCP toolsets, and A2A sub-agent delegates. Uses CDI for dependency injection.
            </p>
        </div>

        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px;">
            <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">AgentCard Producer</h4>
            <p style="font-size: 14px; line-height: 1.6; color: #65676b;">
                Produces the A2A AgentCard describing agent capabilities. Exposed at <code>/agent-card</code> endpoint for discovery by other agents.
            </p>
        </div>

        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px;">
            <h4 style="font-size: 16px; font-weight: 600; margin-bottom: 8px; color: #050505;">Executor Producer</h4>
            <p style="font-size: 14px; line-height: 1.6; color: #65676b;">
                Creates task executor for processing incoming A2A requests. Handles task deserialization, agent invocation, and response serialization.
            </p>
        </div>
    </div>

    <!-- API Reference -->
    <div class="section">
        <h2 style="font-size: 22px; font-weight: 600; margin-bottom: 16px; color: #050505; border-bottom: 2px solid #1877f2; padding-bottom: 8px;">API Reference</h2>
        
        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Tool Management</h3>
        
        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #42b883;">
            <div style="margin-bottom: 8px; font-family: monospace; font-size: 14px; font-weight: 600;">
                <span style="color: #42b883;">GET</span> /api/tools
            </div>
            <p style="color: #65676b; font-size: 14px; margin-bottom: 8px;">List all registered MCP tool servers</p>
            <div style="font-size: 13px; font-family: monospace; background: #ffffff; padding: 8px; border-radius: 4px;">
                Response: Array&lt;ToolMetadata&gt;
            </div>
        </div>

        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #e67e22;">
            <div style="margin-bottom: 8px; font-family: monospace; font-size: 14px; font-weight: 600;">
                <span style="color: #e67e22;">POST</span> /api/tools
            </div>
            <p style="color: #65676b; font-size: 14px; margin-bottom: 8px;">Register a new MCP tool server</p>
            <div style="font-size: 13px; font-family: monospace; background: #ffffff; padding: 8px; border-radius: 4px; margin-bottom: 8px;">
                Request: {name, description, endpointUrl}
            </div>
            <div style="font-size: 13px; font-family: monospace; background: #ffffff; padding: 8px; border-radius: 4px;">
                Response: ToolMetadata
            </div>
        </div>

        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #e74c3c;">
            <div style="margin-bottom: 8px; font-family: monospace; font-size: 14px; font-weight: 600;">
                <span style="color: #e74c3c;">DELETE</span> /api/tools/{toolId}
            </div>
            <p style="color: #65676b; font-size: 14px; margin-bottom: 8px;">Unregister an MCP tool server</p>
            <div style="font-size: 13px; font-family: monospace; background: #ffffff; padding: 8px; border-radius: 4px;">
                Response: 204 No Content
            </div>
        </div>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Agent Management</h3>

        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #42b883;">
            <div style="margin-bottom: 8px; font-family: monospace; font-size: 14px; font-weight: 600;">
                <span style="color: #42b883;">GET</span> /api/a2a-agents
            </div>
            <p style="color: #65676b; font-size: 14px; margin-bottom: 8px;">List all registered A2A agents</p>
            <div style="font-size: 13px; font-family: monospace; background: #ffffff; padding: 8px; border-radius: 4px;">
                Response: Array&lt;A2AAgentMetadata&gt;
            </div>
        </div>

        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #e67e22;">
            <div style="margin-bottom: 8px; font-family: monospace; font-size: 14px; font-weight: 600;">
                <span style="color: #e67e22;">POST</span> /api/a2a-agents/test
            </div>
            <p style="color: #65676b; font-size: 14px; margin-bottom: 8px;">Test connectivity to an A2A agent</p>
            <div style="font-size: 13px; font-family: monospace; background: #ffffff; padding: 8px; border-radius: 4px; margin-bottom: 8px;">
                Request: {url}
            </div>
            <div style="font-size: 13px; font-family: monospace; background: #ffffff; padding: 8px; border-radius: 4px;">
                Response: {healthy, agentCard}
            </div>
        </div>

        <div style="background: #f5f6f7; padding: 16px; border-radius: 8px; margin-bottom: 16px; border-left: 4px solid #e67e22;">
            <div style="margin-bottom: 8px; font-family: monospace; font-size: 14px; font-weight: 600;">
                <span style="color: #e67e22;">POST</span> /api/agents/generate
            </div>
            <p style="color: #65676b; font-size: 14px; margin-bottom: 8px;">Generate and download agent code as ZIP</p>
            <div style="font-size: 13px; font-family: monospace; background: #ffffff; padding: 8px; border-radius: 4px; margin-bottom: 8px;">
                Request: {agentName, description, instructions, packageName, toolIds, subagentIds, googleApiKey, serverPort}
            </div>
            <div style="font-size: 13px; font-family: monospace; background: #ffffff; padding: 8px; border-radius: 4px;">
                Response: application/zip
            </div>
        </div>
    </div>

    <!-- System Requirements -->
    <div class="section">
        <h2 style="font-size: 22px; font-weight: 600; margin-bottom: 16px; color: #050505; border-bottom: 2px solid #1877f2; padding-bottom: 8px;">System Requirements</h2>
        
        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Development Environment</h3>
        <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
            <thead>
                <tr style="background: #f5f6f7;">
                    <th style="padding: 12px; text-align: left; border: 1px solid #e4e6eb; font-weight: 600;">Component</th>
                    <th style="padding: 12px; text-align: left; border: 1px solid #e4e6eb; font-weight: 600;">Version</th>
                    <th style="padding: 12px; text-align: left; border: 1px solid #e4e6eb; font-weight: 600;">Purpose</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Java JDK</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb; font-family: monospace;">17+</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Runtime environment</td>
                </tr>
                <tr>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Maven</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb; font-family: monospace;">3.6+</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Build and dependency management</td>
                </tr>
                <tr>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Redis</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb; font-family: monospace;">6.0+</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Registry storage and pub/sub</td>
                </tr>
                <tr>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Google Gemini API</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb; font-family: monospace;">Latest</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">LLM inference</td>
                </tr>
            </tbody>
        </table>

        <h3 style="font-size: 18px; font-weight: 600; margin: 20px 0 12px; color: #050505;">Key Dependencies</h3>
        <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
            <thead>
                <tr style="background: #f5f6f7;">
                    <th style="padding: 12px; text-align: left; border: 1px solid #e4e6eb; font-weight: 600;">Library</th>
                    <th style="padding: 12px; text-align: left; border: 1px solid #e4e6eb; font-weight: 600;">Version</th>
                    <th style="padding: 12px; text-align: left; border: 1px solid #e4e6eb; font-weight: 600;">Role</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Google ADK</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb; font-family: monospace;">0.3.0</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Agent orchestration framework</td>
                </tr>
                <tr>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">MCP Java SDK</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb; font-family: monospace;">0.12.1</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Model Context Protocol client</td>
                </tr>
                <tr>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">A2A Java SDK</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb; font-family: monospace;">0.3.1.Final</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Agent-to-agent communication</td>
                </tr>
                <tr>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">Dropwizard</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb; font-family: monospace;">3.0.7</td>
                    <td style="padding: 12px; border: 1px solid #e4e6eb;">REST API framework</td>
                </tr>
            </tbody>
        </table>
    </div>
</div>
`;

function copyDiagram(id) {
    const textarea = document.getElementById(id);
    textarea.select();
    document.execCommand('copy');
    showNotification('Diagram XML copied to clipboard', 'success');
}
