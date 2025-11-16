package com.example.agent.registry;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Metadata for A2A agents
 */
public class A2AAgentMetadata {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("protocolVersion")
    private String protocolVersion;
    
    @JsonProperty("capabilities")
    private Map<String, Object> capabilities;
    
    @JsonProperty("skills")
    private List<Map<String, Object>> skills;
    
    @JsonProperty("inputModes")
    private List<String> inputModes;
    
    @JsonProperty("outputModes")
    private List<String> outputModes;
    
    @JsonProperty("status")
    private String status; // "connected", "disconnected", "error"
    
    @JsonProperty("lastUpdated")
    private long lastUpdated;
    
    public A2AAgentMetadata() {
        this.lastUpdated = System.currentTimeMillis();
        this.status = "connected";
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; }
    
    public Map<String, Object> getCapabilities() { return capabilities; }
    public void setCapabilities(Map<String, Object> capabilities) { this.capabilities = capabilities; }
    
    public List<Map<String, Object>> getSkills() { return skills; }
    public void setSkills(List<Map<String, Object>> skills) { this.skills = skills; }
    
    public List<String> getInputModes() { return inputModes; }
    public void setInputModes(List<String> inputModes) { this.inputModes = inputModes; }
    
    public List<String> getOutputModes() { return outputModes; }
    public void setOutputModes(List<String> outputModes) { this.outputModes = outputModes; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}
