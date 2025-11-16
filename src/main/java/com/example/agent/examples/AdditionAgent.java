package com.example.agent.examples;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;

import java.util.Map;

/**
 * Example agent that adds two numbers.
 * This agent can be dynamically loaded and registered via the registry.
 */
public class AdditionAgent {
    
    public static BaseAgent ROOT_AGENT = initAgent();
    
    private static BaseAgent initAgent() {
        return LlmAgent.builder()
            .name("addition-agent")
            .description("Adds two numbers together")
            .instruction("""
                You are a helpful assistant that adds numbers.
                Use the 'add' tool to perform addition operations.
                Always provide clear explanations of your calculations.
                """)
            .model("gemini-2.5-flash")
            .tools(FunctionTool.create(AdditionAgent.class, "add"))
            .build();
    }
    
    /**
     * Add two numbers together
     */
    @Schema(description = "Add two numbers and return the sum")
    public static Map<String, Object> add(
        @Schema(name = "a", description = "First number") double a,
        @Schema(name = "b", description = "Second number") double b) {
        
        double result = a + b;
        
        return Map.of(
            "operation", "addition",
            "operand1", a,
            "operand2", b,
            "result", result,
            "explanation", String.format("%.2f + %.2f = %.2f", a, b, result)
        );
    }
}
