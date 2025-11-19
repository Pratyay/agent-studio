#!/bin/bash

# Test the chat API endpoint

echo "1. Checking if server is running..."
curl -s http://localhost:8080/api/a2a-agents > /dev/null && echo "✓ Server is running" || echo "✗ Server is NOT running"

echo -e "\n2. Listing registered agents..."
AGENTS=$(curl -s http://localhost:8080/api/a2a-agents)
echo "$AGENTS" | python3 -m json.tool 2>/dev/null || echo "$AGENTS"

# Extract first agent ID if any exist
AGENT_ID=$(echo "$AGENTS" | python3 -c "import sys, json; agents=json.load(sys.stdin); print(agents[0]['id'] if agents else '')" 2>/dev/null)

if [ -z "$AGENT_ID" ]; then
    echo -e "\n✗ No agents registered. Please register an A2A agent first via the UI."
    echo "   URL: http://localhost:8080/ui/unified-builder.html"
    exit 1
fi

echo -e "\n3. Found agent ID: $AGENT_ID"
echo -e "\n4. Testing chat endpoint..."
RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello, test message"}' \
  "http://localhost:8080/api/a2a-agents/${AGENT_ID}/message")

echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"

if echo "$RESPONSE" | grep -q "\"response\""; then
    echo -e "\n✓ Chat API is working!"
else
    echo -e "\n✗ Chat API returned an error"
fi
