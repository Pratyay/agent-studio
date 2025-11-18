package com.example.agent.registry;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for an agent callback.
 */
public class CallbackMetadata {
    
    @JsonProperty("callback_id")
    private String callbackId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("fqdn")
    private String fqdn;  // Fully Qualified Domain Name of the callback class
    
    @JsonProperty("type")
    private String type;  // BEFORE_AGENT or AFTER_AGENT
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("created_at")
    private Long createdAt;
    
    @JsonProperty("updated_at")
    private Long updatedAt;
    
    public CallbackMetadata() {
        this.tags = new ArrayList<>();
    }
    
    public CallbackMetadata(String callbackId, String name, String description, String fqdn, String type, List<String> tags) {
        this.callbackId = callbackId;
        this.name = name;
        this.description = description;
        this.fqdn = fqdn;
        this.type = type;
        this.tags = tags != null ? tags : new ArrayList<>();
    }
    
    // Getters and Setters
    
    public String getCallbackId() {
        return callbackId;
    }
    
    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
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
    
    public String getFqdn() {
        return fqdn;
    }
    
    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
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
}
