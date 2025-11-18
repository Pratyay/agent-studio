package com.example.agent.callbacks;

import com.google.adk.agents.Callbacks.BeforeAgentCallback;
import com.google.adk.agents.CallbackContext;
import com.google.genai.types.Content;
import io.reactivex.rxjava3.core.Maybe;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Callback that collects metrics about agent usage.
 * Tracks message counts, user activity, and other statistics.
 */
public class MetricsCallback implements BeforeAgentCallback {
    
    private static final AtomicLong totalMessages = new AtomicLong(0);
    private static final ConcurrentHashMap<String, AtomicLong> messagesPerUser = new ConcurrentHashMap<>();
    
    @Override
    public Maybe<Content> call(CallbackContext context) {
        // Increment total message count
        long count = totalMessages.incrementAndGet();
        
        // Use session ID as a proxy for tracking (since user ID isn't directly available)
        String sessionId = context.sessionId();
        messagesPerUser.computeIfAbsent(sessionId, k -> new AtomicLong(0)).incrementAndGet();
        
        System.out.println("[MetricsCallback] Total messages processed: " + count);
        System.out.println("[MetricsCallback] Messages in session " + sessionId + ": " + 
                          messagesPerUser.get(sessionId).get());
        System.out.println("[MetricsCallback] Agent: " + context.agentName());
        
        // Return empty to continue with original content
        return Maybe.empty();
    }
    
    /**
     * Get total number of messages processed.
     */
    public static long getTotalMessages() {
        return totalMessages.get();
    }
    
    /**
     * Get number of messages from a specific user.
     */
    public static long getUserMessages(String userId) {
        AtomicLong count = messagesPerUser.get(userId);
        return count != null ? count.get() : 0;
    }
}
