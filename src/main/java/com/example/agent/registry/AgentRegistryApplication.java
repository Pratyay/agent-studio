package com.example.agent.registry;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.assets.AssetsBundle;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

/**
 * Dropwizard application for the Agent Registry service.
 * This provides a REST API for managing dynamically loaded agents.
 */
public class AgentRegistryApplication extends Application<AgentRegistryConfiguration> {
    
    public static void main(String[] args) throws Exception {
        new AgentRegistryApplication().run(args);
    }
    
    @Override
    public String getName() {
        return "agent-registry";
    }
    
    @Override
    public void initialize(Bootstrap<AgentRegistryConfiguration> bootstrap) {
        // Serve static assets from /static classpath directory at /ui URI path
        bootstrap.addBundle(new AssetsBundle("/static", "/ui", "index.html", "assets"));
    }
    
    @Override
    public void run(AgentRegistryConfiguration configuration, Environment environment) {
        // Create core components
        final AgentRegistry registry = new AgentRegistry(
            configuration.getRedisHost(), 
            configuration.getRedisPort()
        );
        final ToolRegistry toolRegistry = new ToolRegistry(
            configuration.getRedisHost(),
            configuration.getRedisPort()
        );
        final CallbackRegistry callbackRegistry = new CallbackRegistry(
            configuration.getRedisHost(),
            configuration.getRedisPort()
        );
        final DynamicAgentLoader loader = new DynamicAgentLoader(registry);
        final RegistryAwareRouter router = new RegistryAwareRouter(registry, loader);
        final A2AClientService a2aClientService = new A2AClientService(
            configuration.getRedisHost(),
            configuration.getRedisPort()
        );
        
        // Register resources
        final RootRedirectResource rootRedirect = new RootRedirectResource();
        final AgentRegistryResource agentResource = new AgentRegistryResource(registry, loader, router);
        final ToolRegistryResource toolResource = new ToolRegistryResource(toolRegistry);
        final CallbackRegistryResource callbackResource = new CallbackRegistryResource(callbackRegistry);
        final MCPDiscoveryResource mcpDiscoveryResource = new MCPDiscoveryResource();
        final AgentGeneratorResource generatorResource = new AgentGeneratorResource(toolRegistry, a2aClientService);
        final A2AAgentResource a2aAgentResource = new A2AAgentResource(a2aClientService);
        environment.jersey().register(rootRedirect);
        environment.jersey().register(agentResource);
        environment.jersey().register(toolResource);
        environment.jersey().register(callbackResource);
        environment.jersey().register(mcpDiscoveryResource);
        environment.jersey().register(generatorResource);
        environment.jersey().register(a2aAgentResource);
        
        // Configure CORS
        configureCors(environment);
        
        // Add shutdown hook
        environment.lifecycle().manage(new io.dropwizard.lifecycle.Managed() {
            @Override
            public void start() {
                // Already started
            }
            
            @Override
            public void stop() {
                registry.close();
                toolRegistry.close();
                callbackRegistry.close();
                a2aClientService.close();
            }
        });
    }
    private void configureCors(Environment environment) {
        final FilterRegistration.Dynamic cors = environment.servlets()
            .addFilter("CORS", CrossOriginFilter.class);
        
        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,PUT,DELETE,OPTIONS");
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
        
        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }
}
