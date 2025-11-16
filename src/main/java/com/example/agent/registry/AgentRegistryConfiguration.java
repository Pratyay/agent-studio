package com.example.agent.registry;

import io.dropwizard.core.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotEmpty;

/**
 * Dropwizard configuration for the Agent Registry application.
 */
public class AgentRegistryConfiguration extends Configuration {
    
    @NotEmpty
    @JsonProperty
    private String redisHost = "localhost";
    
    @JsonProperty
    private int redisPort = 6379;
    
    public String getRedisHost() {
        return redisHost;
    }
    
    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }
    
    public int getRedisPort() {
        return redisPort;
    }
    
    public void setRedisPort(int redisPort) {
        this.redisPort = redisPort;
    }
}
