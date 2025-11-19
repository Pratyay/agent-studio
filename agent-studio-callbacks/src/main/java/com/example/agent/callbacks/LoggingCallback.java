package com.example.agent.callbacks;

import com.google.adk.agents.Callbacks.BeforeAgentCallback;
import com.google.adk.agents.CallbackContext;
import com.google.genai.types.Content;
import io.reactivex.rxjava3.core.Maybe;
import jakarta.enterprise.context.ApplicationScoped;

import javax.inject.Inject;
import java.time.Instant;

/**
 * Callback that logs all incoming messages before the agent processes them.
 * Useful for debugging and audit trails.
 */

@ApplicationScoped
public class LoggingCallback implements BeforeAgentCallback {

    @Inject
    public LoggingCallback() {
    }

    @Override
    public Maybe<Content> call(CallbackContext context) {
        System.out.println("[LoggingCallback] Timestamp: " + Instant.now());
        System.out.println("[LoggingCallback] Session ID: " + context.sessionId());
        System.out.println("[LoggingCallback] Agent Name: " + context.agentName());
        System.out.println("[LoggingCallback] Invocation ID: " + context.invocationId());
        context.userContent().ifPresent(content -> 
            System.out.println("[LoggingCallback] User Content: " + content));
        
        // Return empty to continue with original content
        return Maybe.empty();
    }
}
