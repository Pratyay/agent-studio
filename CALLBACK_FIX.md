# Callback Integration Fix

## Problem
When selecting callbacks in the UI, they were not being added to the generated agent code.

## Root Cause
The UI was properly collecting selected callbacks and sending them in the generation request as `beforeAgentCallbacks` and `afterAgentCallbacks`, but the backend `AgentGeneratorResource.GenerateRequest` class was missing these fields, causing them to be silently ignored.

## Solution
Added the missing fields to the backend:

### Changes Made

1. **AgentGeneratorResource.java** - Added callback fields to `GenerateRequest`:
   ```java
   public List<String> beforeAgentCallbacks;
   public List<String> afterAgentCallbacks;
   ```

2. **AgentGeneratorResource.java** - Pass callbacks to code generator:
   ```java
   genRequest.beforeAgentCallbacks = request.beforeAgentCallbacks != null ? request.beforeAgentCallbacks : new ArrayList<>();
   genRequest.afterAgentCallbacks = request.afterAgentCallbacks != null ? request.afterAgentCallbacks : new ArrayList<>();
   ```

## How It Works

### UI Flow (unified-builder.html)
1. User checks callback boxes in the "Agent Callbacks" section
2. Selected callbacks are stored in `selectedCallbacks` array
3. When generating agent, callbacks are sent as:
   ```javascript
   beforeAgentCallbacks: selectedCallbacks,
   afterAgentCallbacks: []
   ```

### Backend Flow (AgentGeneratorResource.java)
1. Receives callback FQDNs in the request
2. Passes them to `AgentCodeGenerator.GenerationRequest`
3. Code generator:
   - Imports callback classes (lines 137-147 in AgentCodeGenerator.java)
   - Adds callbacks to agent builder (lines 261-287 in AgentCodeGenerator.java)
   - Includes callback dependency in pom.xml (lines 628-635 in AgentCodeGenerator.java)

### Generated Code Example
When callbacks are selected, the generated agent code will include:

```java
// Imports
import com.example.agent.callbacks.LoggingCallback;
import com.example.agent.callbacks.MetricsCallback;

// Agent builder
return LlmAgent.builder()
    .name("my-agent")
    .description("...")
    .instruction("...")
    .model("gemini-2.5-flash")
    .tools(allTools)
    .beforeAgentCallbacks(List.of(
        new LoggingCallback(),
        new MetricsCallback()
    ))
    .build();
```

## Available Callbacks

All callbacks are from the `agent-studio-callbacks` library:

1. **LoggingCallback** (`com.example.agent.callbacks.LoggingCallback`)
   - Logs all messages with timestamp, session ID, agent name
   - Tags: logging, audit, debugging

2. **MetricsCallback** (`com.example.agent.callbacks.MetricsCallback`)
   - Collects usage metrics and per-session counts
   - Tags: metrics, analytics, monitoring

3. **SecurityCallback** (`com.example.agent.callbacks.SecurityCallback`)
   - Blocks malicious content (script tags, eval, etc.)
   - Tags: security, validation, filtering

4. **RateLimitCallback** (`com.example.agent.callbacks.RateLimitCallback`)
   - Enforces rate limiting (10 msgs/min per session)
   - Tags: rate-limiting, throttling, protection

## Testing

To verify the fix:

1. Start the server: `java -jar target/adk-agents-1.0-SNAPSHOT.jar server config.yml`
2. Open browser: `http://localhost:8080`
3. Build an agent and select one or more callbacks
4. Generate and download the agent ZIP
5. Verify the generated code includes:
   - Callback imports at the top
   - `.beforeAgentCallbacks(List.of(...))` in the agent builder
   - `agent-studio-callbacks` dependency in pom.xml

## Build Status
✅ Fix implemented and built successfully
