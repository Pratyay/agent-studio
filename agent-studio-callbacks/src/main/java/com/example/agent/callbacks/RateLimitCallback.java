package com.example.agent.callbacks;

import com.google.adk.agents.Callbacks.BeforeAgentCallback;
import com.google.adk.agents.CallbackContext;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Maybe;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Callback that enforces rate limiting on a per-user basis.
 * Blocks users who exceed the configured message limit within the time window.
 */
public class RateLimitCallback implements BeforeAgentCallback {
    
    private static final int MAX_MESSAGES_PER_MINUTE = 10;
    private static final long TIME_WINDOW_SECONDS = 60;
    
    private static class RateLimitInfo {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart = Instant.now().getEpochSecond();
    }
    
    private static final ConcurrentHashMap<String, RateLimitInfo> rateLimits = new ConcurrentHashMap<>();
    
    @Override
    public Maybe<Content> call(CallbackContext context) {
        // Use session ID for rate limiting
        String sessionId = context.sessionId();
        long currentTime = Instant.now().getEpochSecond();
        
        RateLimitInfo info = rateLimits.computeIfAbsent(sessionId, k -> new RateLimitInfo());
        
        synchronized (info) {
            // Check if we need to reset the window
            if (currentTime - info.windowStart >= TIME_WINDOW_SECONDS) {
                info.count.set(0);
                info.windowStart = currentTime;
            }
            
            // Increment count
            int currentCount = info.count.incrementAndGet();
            
            // Check if rate limit exceeded
            if (currentCount > MAX_MESSAGES_PER_MINUTE) {
                long secondsRemaining = TIME_WINDOW_SECONDS - (currentTime - info.windowStart);
                
                System.out.println("[RateLimitCallback] RATE LIMIT EXCEEDED for session: " + sessionId);
                System.out.println("[RateLimitCallback] Current count: " + currentCount + 
                                 " (limit: " + MAX_MESSAGES_PER_MINUTE + ")");
                
                // Return rate limit error message
                Content rateLimitContent = Content.fromParts(
                    Part.fromText("Rate limit exceeded. You have sent too many messages. " +
                                "Please wait " + secondsRemaining + " seconds before trying again.")
                );
                return Maybe.just(rateLimitContent);
            }
            
            System.out.println("[RateLimitCallback] Session " + sessionId + 
                             " - Message " + currentCount + "/" + MAX_MESSAGES_PER_MINUTE);
        }
        
        // Return empty to continue with original content
        return Maybe.empty();
    }
}
