package com.example.agent.registry;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata describing a dynamically loaded agent.
 */
public class AgentMetadata {
    
    @JsonProperty("agent_id")
    private String agentId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("capabilities")
    private List<String> capabilities;
    
    @JsonProperty("jar_path")
    private String jarPath;
    
    @JsonProperty("main_class")
    private String mainClass;
    
    @JsonProperty("tool_definitions")
    private List<ToolDefinition> toolDefinitions;
    
    @JsonProperty("status")
    private AgentStatus status;
    
    @JsonProperty("created_at")
    private Long createdAt;
    
    @JsonProperty("updated_at")
    private Long updatedAt;
    
    @JsonProperty("config")
    private Map<String, Object> config;
    
    // Attached MCP tools (references to ToolRegistry)
    @JsonProperty("attached_tools")
    private List<AttachedTool> attachedTools;
    
    // Sub-agents (references to other agents)
    @JsonProperty("sub_agents")
    private List<SubAgentReference> subAgents;
    
    public AgentMetadata() {
        this.capabilities = new ArrayList<>();
        this.toolDefinitions = new ArrayList<>();
        this.config = new HashMap<>();
        this.attachedTools = new ArrayList<>();
        this.subAgents = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public List<String> getCapabilities() {
        return capabilities;
    }
    
    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }
    
    public String getJarPath() {
        return jarPath;
    }
    
    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }
    
    public String getMainClass() {
        return mainClass;
    }
    
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
    
    public List<ToolDefinition> getToolDefinitions() {
        return toolDefinitions;
    }
    
    public void setToolDefinitions(List<ToolDefinition> toolDefinitions) {
        this.toolDefinitions = toolDefinitions;
    }
    
    public AgentStatus getStatus() {
        return status;
    }
    
    public void setStatus(AgentStatus status) {
        this.status = status;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Map<String, Object> getConfig() {
        return config;
    }
    
    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
    
    public List<AttachedTool> getAttachedTools() {
        return attachedTools;
    }
    
    public void setAttachedTools(List<AttachedTool> attachedTools) {
        this.attachedTools = attachedTools;
    }
    
    public List<SubAgentReference> getSubAgents() {
        return subAgents;
    }
    
    public void setSubAgents(List<SubAgentReference> subAgents) {
        this.subAgents = subAgents;
    }
    
    /**
     * Tool definition for agent capabilities
     */
    public static class ToolDefinition {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("parameters")
        private List<ParameterDefinition> parameters;
        
        @JsonProperty("return_type")
        private String returnType;
        
        public ToolDefinition() {
            this.parameters = new ArrayList<>();
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public List<ParameterDefinition> getParameters() {
            return parameters;
        }
        
        public void setParameters(List<ParameterDefinition> parameters) {
            this.parameters = parameters;
        }
        
        public String getReturnType() {
            return returnType;
        }
        
        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }
    }
    
    /**
     * Parameter definition for tools
     */
    public static class ParameterDefinition {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("required")
        private boolean required;
        
        public ParameterDefinition() {
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public void setRequired(boolean required) {
            this.required = required;
        }
    }
    
    /**
     * Agent status enum
     */
    public enum AgentStatus {
        ACTIVE,
        INACTIVE,
        LOADING,
        ERROR
    }
    
    /**
     * Reference to an attached MCP tool from the Tool Registry
     */
    public static class AttachedTool {
        @JsonProperty("tool_id")
        private String toolId;
        
        @JsonProperty("tool_name")
        private String toolName;
        
        @JsonProperty("alias")
        private String alias;  // Optional custom name in agent context
        
        @JsonProperty("config_overrides")
        private Map<String, Object> configOverrides;  // Agent-specific config
        
        @JsonProperty("enabled")
        private boolean enabled;
        
        public AttachedTool() {
            this.configOverrides = new HashMap<>();
            this.enabled = true;
        }
        
        public String getToolId() {
            return toolId;
        }
        
        public void setToolId(String toolId) {
            this.toolId = toolId;
        }
        
        public String getToolName() {
            return toolName;
        }
        
        public void setToolName(String toolName) {
            this.toolName = toolName;
        }
        
        public String getAlias() {
            return alias;
        }
        
        public void setAlias(String alias) {
            this.alias = alias;
        }
        
        public Map<String, Object> getConfigOverrides() {
            return configOverrides;
        }
        
        public void setConfigOverrides(Map<String, Object> configOverrides) {
            this.configOverrides = configOverrides;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    /**
     * Reference to a sub-agent (agent composition)
     */
    public static class SubAgentReference {
        @JsonProperty("agent_id")
        private String agentId;
        
        @JsonProperty("agent_name")
        private String agentName;
        
        @JsonProperty("invocation_pattern")
        private InvocationPattern invocationPattern;
        
        @JsonProperty("delegation_rules")
        private List<String> delegationRules;  // Conditions for delegating to this sub-agent
        
        @JsonProperty("enabled")
        private boolean enabled;
        
        public SubAgentReference() {
            this.delegationRules = new ArrayList<>();
            this.enabled = true;
        }
        
        public String getAgentId() {
            return agentId;
        }
        
        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }
        
        public String getAgentName() {
            return agentName;
        }
        
        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }
        
        public InvocationPattern getInvocationPattern() {
            return invocationPattern;
        }
        
        public void setInvocationPattern(InvocationPattern invocationPattern) {
            this.invocationPattern = invocationPattern;
        }
        
        public List<String> getDelegationRules() {
            return delegationRules;
        }
        
        public void setDelegationRules(List<String> delegationRules) {
            this.delegationRules = delegationRules;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    /**
     * Pattern for invoking sub-agents
     */
    public enum InvocationPattern {
        SEQUENTIAL,      // Call sub-agents in order
        PARALLEL,        // Call sub-agents simultaneously
        CONDITIONAL,     // Call based on conditions
        ON_DEMAND        // Call when explicitly requested
    }
}
