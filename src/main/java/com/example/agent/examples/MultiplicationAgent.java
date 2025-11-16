package com.example.agent.examples;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;

import java.util.Map;

/**
 * Example agent that multiplies two numbers.
 * This agent can be dynamically loaded and registered via the registry.
 */
public class MultiplicationAgent {
    
    public static BaseAgent ROOT_AGENT = initAgent();
    
    private static BaseAgent initAgent() {
        return LlmAgent.builder()
            .name("multiplication-agent")
            .description("Multiplies two numbers together")
            .instruction("""
                You are a helpful assistant that multiplies numbers.
                Use the 'multiply' tool to perform multiplication operations.
                Always provide clear explanations of your calculations.
                """)
            .model("gemini-2.5-flash")
            .tools(FunctionTool.create(MultiplicationAgent.class, "multiply"))
            .build();
    }
    
    /**
     * Multiply two numbers together
     */
    @Schema(description = "Multiply two numbers and return the product")
    public static Map<String, Object> multiply(
        @Schema(name = "a", description = "First number") double a,
        @Schema(name = "b", description = "Second number") double b) {
        
        double result = a * b;
        
        return Map.of(
            "operation", "multiplication",
            "operand1", a,
            "operand2", b,
            "result", result,
            "explanation", String.format("%.2f × %.2f = %.2f", a, b, result)
        );
    }
}
