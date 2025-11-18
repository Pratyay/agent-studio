# Agent Studio Callbacks

A library of pre-built callbacks for Agent Studio generated agents. Callbacks allow you to add custom behavior before and after agent execution without modifying the core agent code.

## Available Callbacks

### BeforeAgentCallback

Callbacks that execute **before** the agent processes a message.

**Note:** All callbacks use the existing Google ADK callback interfaces from `com.google.adk.agents.Callbacks`.

#### 1. LoggingCallback
**FQDN:** `com.example.agent.callbacks.LoggingCallback`

Logs all incoming messages with timestamp, user ID, and session ID. Useful for debugging and audit trails.

```java
System.out.println("[LoggingCallback] Timestamp: 2025-11-18T06:15:30Z");
System.out.println("[LoggingCallback] User ID: user123");
System.out.println("[LoggingCallback] Session ID: session-abc-123");
System.out.println("[LoggingCallback] Content: ...");
```

#### 2. MetricsCallback
**FQDN:** `com.example.agent.callbacks.MetricsCallback`

Collects usage metrics including total message count and per-user message counts.

```java
System.out.println("[MetricsCallback] Total messages processed: 42");
System.out.println("[MetricsCallback] Messages from user user123: 5");
```

**Static Methods:**
- `MetricsCallback.getTotalMessages()` - Get total message count
- `MetricsCallback.getUserMessages(String userId)` - Get message count for specific user

#### 3. SecurityCallback
**FQDN:** `com.example.agent.callbacks.SecurityCallback`

Performs basic security checks on incoming messages. Blocks messages containing:
- Script tags (`<script>`)
- JavaScript protocol (`javascript:`)
- eval() calls
- Event handlers (`onclick=`, etc.)

If blocked, returns a safe error message instead of passing the content to the agent.

#### 4. RateLimitCallback
**FQDN:** `com.example.agent.callbacks.RateLimitCallback`

Enforces rate limiting on a per-user basis:
- **Default limit:** 10 messages per minute
- **Configurable:** Modify `MAX_MESSAGES_PER_MINUTE` and `TIME_WINDOW_SECONDS` constants

Returns an error message with time remaining if rate limit is exceeded.

### AfterAgentCallback

Callbacks that execute **after** the agent completes processing.

Currently, the library provides the base interface. You can implement custom after-agent callbacks for:
- Response filtering
- Response transformation
- Response logging
- Analytics

## Usage in Agent Studio

When creating an agent in Agent Studio, you can attach callbacks by providing their FQDNs:

### Example Request to AgentCodeGenerator

```json
{
  "agentName": "MyAgent",
  "description": "My custom agent",
  "packageName": "com.example.myagent",
  "beforeAgentCallbacks": [
    "com.example.agent.callbacks.LoggingCallback",
    "com.example.agent.callbacks.MetricsCallback",
    "com.example.agent.callbacks.SecurityCallback",
    "com.example.agent.callbacks.RateLimitCallback"
  ],
  "afterAgentCallbacks": []
}
```

### Generated Code

The generator will:
1. Add `agent-studio-callbacks` dependency to the generated agent's `pom.xml`
2. Import the specified callback classes
3. Instantiate and attach them to the agent

```java
import com.example.agent.callbacks.LoggingCallback;
import com.example.agent.callbacks.MetricsCallback;
import com.example.agent.callbacks.SecurityCallback;
import com.example.agent.callbacks.RateLimitCallback;

// In agent builder
LlmAgent.builder()
    .name("my-agent")
    .description("My custom agent")
    .beforeAgentCallbacks(List.of(
        new LoggingCallback(),
        new MetricsCallback(),
        new SecurityCallback(),
        new RateLimitCallback()
    ))
    .build();
```

## Creating Custom Callbacks

You can create your own callbacks by implementing the interfaces:

### BeforeAgentCallback Example

```java
package com.example.custom;

import com.example.agent.callbacks.BeforeAgentCallback;
import com.example.agent.callbacks.CallbackContext;
import com.google.genai.types.Content;
import io.reactivex.rxjava3.core.Maybe;

public class CustomCallback implements BeforeAgentCallback {
    @Override
    public Maybe<Content> call(CallbackContext context) {
        // Your custom logic here
        System.out.println("Custom callback executed!");
        
        // Return empty to continue with original content
        return Maybe.empty();
        
        // Or return modified/replacement content
        // return Maybe.just(modifiedContent);
    }
}
```

### Using Custom Callbacks

1. Create your callback class
2. Package it as a JAR and install to Maven repository
3. Provide the FQDN when creating an agent in Agent Studio

## CallbackContext

The `CallbackContext` object passed to callbacks contains:

- `Content getContent()` - The message content
- `String getSessionId()` - The session ID
- `String getUserId()` - The user ID
- `Map<String, Object> getMetadata()` - Custom metadata
- `void putMetadata(String key, Object value)` - Add custom metadata

Callbacks can use metadata to pass information between callbacks.

## Installation

The library is automatically installed when you build the callbacks module:

```bash
cd agent-studio-callbacks
mvn clean install
```

This installs `agent-studio-callbacks-1.0.0.jar` to your local Maven repository.

## Dependencies

- **Google ADK** 0.3.0 (provided)
- **RxJava3** 3.1.8 (provided)

Dependencies are marked as `provided` since they're already included in generated agents.

## License

[Specify your license here]
