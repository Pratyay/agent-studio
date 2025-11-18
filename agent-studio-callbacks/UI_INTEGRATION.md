# UI Integration Guide for Callbacks

This guide explains how to integrate callback selection in the Agent Studio UI.

## Backend Changes (Completed)

### 1. GenerationRequest Updates
The `AgentCodeGenerator.GenerationRequest` class now includes:

```java
public List<String> beforeAgentCallbacks;  // FQDNs of before-agent callbacks
public List<String> afterAgentCallbacks;   // FQDNs of after-agent callbacks
```

### 2. Code Generation
When callbacks are specified:
- Imports are automatically added to the generated agent code
- Callbacks are instantiated and attached to the agent builder
- The `agent-studio-callbacks` dependency is added to `pom.xml` (only if callbacks are selected)

## UI Changes Needed

### 1. Add Callback Selection to Agent Builder UI

Add a new section in the agent configuration panel:

```html
<div class="callbacks-section">
    <h3>🔔 Callbacks</h3>
    
    <div class="callback-group">
        <label>Before Agent Execution:</label>
        <div class="checkbox-list">
            <label>
                <input type="checkbox" value="com.example.agent.callbacks.LoggingCallback">
                Logging - Log all incoming messages
            </label>
            <label>
                <input type="checkbox" value="com.example.agent.callbacks.MetricsCallback">
                Metrics - Track usage statistics
            </label>
            <label>
                <input type="checkbox" value="com.example.agent.callbacks.SecurityCallback">
                Security - Block suspicious content
            </label>
            <label>
                <input type="checkbox" value="com.example.agent.callbacks.RateLimitCallback">
                Rate Limiting - 10 messages/minute per user
            </label>
        </div>
    </div>
    
    <div class="callback-group">
        <label>After Agent Execution:</label>
        <div class="checkbox-list">
            <em>No pre-built callbacks available yet. You can add custom ones.</em>
        </div>
    </div>
</div>
```

### 2. Update JavaScript to Collect Callback Selections

```javascript
// In your agent generation function
function generateAgent() {
    // ... existing code ...
    
    // Collect selected callbacks
    const beforeCallbacks = [];
    document.querySelectorAll('.callback-group:nth-child(2) input[type="checkbox"]:checked')
        .forEach(cb => beforeCallbacks.push(cb.value));
    
    const afterCallbacks = [];
    document.querySelectorAll('.callback-group:nth-child(3) input[type="checkbox"]:checked')
        .forEach(cb => afterCallbacks.push(cb.value));
    
    const requestBody = {
        agentName: agentName,
        description: description,
        instructions: instructions,
        packageName: packageName,
        attachedTools: attachedTools,
        subagents: subagents,
        googleApiKey: googleApiKey,
        serverPort: serverPort,
        model: model,
        beforeAgentCallbacks: beforeCallbacks,
        afterAgentCallbacks: afterCallbacks
    };
    
    // Send to backend
    fetch('/api/generate-agent', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
    })
    .then(response => response.blob())
    .then(blob => {
        // Download the generated ZIP
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = agentName.toLowerCase().replace(/\\s+/g, '-') + '-agent.zip';
        a.click();
    });
}
```

### 3. Add Callback Information Tooltips (Optional)

```html
<label>
    <input type="checkbox" value="com.example.agent.callbacks.LoggingCallback">
    Logging
    <span class="info-tooltip" title="Logs all incoming messages with timestamp, user ID, and session ID">ℹ️</span>
</label>
```

### 4. Styling Suggestions

```css
.callbacks-section {
    margin-top: 20px;
    padding: 15px;
    border: 1px solid #ddd;
    border-radius: 5px;
}

.callback-group {
    margin-bottom: 15px;
}

.checkbox-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-top: 8px;
}

.checkbox-list label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
}

.info-tooltip {
    cursor: help;
    font-size: 12px;
    color: #666;
}
```

## Example API Request

After UI integration, the request to generate an agent will look like:

```json
{
  "agentName": "CustomerSupport",
  "description": "AI customer support agent",
  "instructions": "Be helpful and professional",
  "packageName": "com.example.customersupport",
  "attachedTools": [],
  "subagents": [],
  "googleApiKey": "your-api-key",
  "serverPort": 8000,
  "model": "gemini-2.5-flash",
  "beforeAgentCallbacks": [
    "com.example.agent.callbacks.LoggingCallback",
    "com.example.agent.callbacks.MetricsCallback",
    "com.example.agent.callbacks.RateLimitCallback"
  ],
  "afterAgentCallbacks": []
}
```

## Generated Agent Output

The generated agent will automatically include:

### 1. Dependency in pom.xml
```xml
<dependency>
    <groupId>com.example.agent</groupId>
    <artifactId>agent-studio-callbacks</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Imports in Agent Class
```java
import com.example.agent.callbacks.LoggingCallback;
import com.example.agent.callbacks.MetricsCallback;
import com.example.agent.callbacks.RateLimitCallback;
```

### 3. Callback Registration in Agent Builder
```java
return LlmAgent.builder()
    .name("customersupport")
    .description("AI customer support agent")
    .instruction("""
        Be helpful and professional
        """)
    .model("gemini-2.5-flash")
    .beforeAgentCallbacks(List.of(
        new LoggingCallback(),
        new MetricsCallback(),
        new RateLimitCallback()
    ))
    .build();
```

## Available Callbacks

### Before Agent Callbacks

| Callback | FQDN | Description |
|----------|------|-------------|
| Logging | `com.example.agent.callbacks.LoggingCallback` | Logs all messages with metadata |
| Metrics | `com.example.agent.callbacks.MetricsCallback` | Tracks usage statistics |
| Security | `com.example.agent.callbacks.SecurityCallback` | Blocks suspicious content |
| Rate Limit | `com.example.agent.callbacks.RateLimitCallback` | Enforces rate limits (10/min) |

### After Agent Callbacks

Currently, no pre-built callbacks are available for after-agent execution. Users can create custom ones.

## Testing

1. Select callbacks in the UI
2. Generate an agent
3. Extract the ZIP file
4. Run `mvn clean package` to verify compilation
5. Run the agent and observe callback output in logs

## Future Enhancements

1. **Dynamic Callback Discovery**: Auto-populate available callbacks from a registry
2. **Callback Configuration**: Allow users to configure callback parameters (e.g., rate limit threshold)
3. **Custom Callback Upload**: Allow users to upload their own callback JARs
4. **Callback Preview**: Show example output for each callback
5. **Callback Ordering**: Allow users to reorder callback execution
