package com.example.agent.callbacks;

import com.google.adk.agents.Callbacks.BeforeAgentCallback;
import com.google.adk.agents.CallbackContext;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Maybe;
import jakarta.enterprise.context.ApplicationScoped;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Callback that performs basic security checks on incoming messages.
 * Blocks messages containing suspicious patterns or content.
 */
@ApplicationScoped
public class SecurityCallback implements BeforeAgentCallback {
    
    // List of patterns that should be blocked
    private static final List<Pattern> BLOCKED_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)<script.*?>.*?</script>", Pattern.DOTALL),  // Script tags
        Pattern.compile("(?i)javascript:", Pattern.CASE_INSENSITIVE),     // JS protocol
        Pattern.compile("(?i)eval\\s*\\(", Pattern.CASE_INSENSITIVE),     // eval() calls
        Pattern.compile("(?i)on\\w+\\s*=", Pattern.CASE_INSENSITIVE)      // Event handlers
    );

    @Inject
    public SecurityCallback() {
    }

    @Override
    public Maybe<Content> call(CallbackContext context) {
        // Get user content if present
        if (context.userContent().isEmpty()) {
            return Maybe.empty();
        }
        
        String contentText = context.userContent().get().toString();
        
        // Check for blocked patterns
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(contentText).find()) {
                System.out.println("[SecurityCallback] BLOCKED: Suspicious content detected");
                System.out.println("[SecurityCallback] Pattern matched: " + pattern.pattern());
                
                // Return a safe error message
                Content blockedContent = Content.fromParts(
                    Part.fromText("Your message has been blocked due to security concerns. " +
                                "Please rephrase your request without potentially harmful content.")
                );
                return Maybe.just(blockedContent);
            }
        }
        
        System.out.println("[SecurityCallback] Content passed security checks");
        
        // Return empty to continue with original content
        return Maybe.empty();
    }
}
