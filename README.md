# MCP AI Studio

This project is a Spring Boot API + UI demo that triggers an MCP-style workflow, sends the MCP payload to OpenAI for refinement, and returns the final result to the user.

## What this project does

- Accepts a prompt through REST
- Calls a real or testing MCP endpoint
- Uses the response as the source of truth for business data
- Sends the prompt and MCP payload to OpenAI
- Returns the raw MCP payload plus the AI result
- Renders a Streamlit-like dashboard for testing

## Important security note

Do not hard-code API keys in source files. Use environment variables only.

## Prerequisites

- Java 21+
- Maven 3.9+
- An OpenAI API key for the AI path

## Configuration

The app uses these defaults in `src/main/resources/application.properties`:

```properties
server.port=8080
spring.config.import=optional:file:.env[.properties]
mcp.server-url=${MCP_SERVER_URL:http://localhost:${server.port}/mcp}

openai.api-key=${OPENAI_API_KEY:}
openai.model=${OPENAI_MODEL:gpt-4o-mini}
openai.organization=${OPENAI_ORG:}
spring.jackson.default-property-inclusion=non_null
```

This means:

- If `MCP_SERVER_URL` is not set, the app calls its own local test MCP endpoint at `http://localhost:8080/mcp`.
- This avoids the earlier `Connection refused` issue caused by a non-existent `localhost:8081` server.
- If a real external MCP server is available, you can override it by setting `MCP_SERVER_URL`.
- The app keeps the existing smart local fallback logic unchanged.

## Setup

On Windows PowerShell:

```powershell
$env:OPENAI_API_KEY="your-openai-secret-key"
$env:OPENAI_MODEL="gpt-4o-mini"
$env:MCP_SERVER_URL="http://localhost:8080/mcp"
mvn spring-boot:run
```

On macOS/Linux:

```bash
export OPENAI_API_KEY="your-openai-secret-key"
export OPENAI_MODEL="gpt-4o-mini"
export MCP_SERVER_URL="http://localhost:8080/mcp"
mvn spring-boot:run
```

## MCP test URL and verification

The default local MCP test URL is:

```text
http://localhost:8080/mcp
```

This endpoint exists inside the same app and returns a structured MCP payload for testing. It simulates a real MCP server without forcing you to run a separate service.

Example payload from the local test endpoint:

```json
{
  "tool": "mcp-local-test",
  "status": "success",
  "request": "Check support tickets for payment issue and SLA risk.",
  "context": "User wants support analytics.",
  "intent": "support",
  "entities": ["Check", "support", "tickets", "payment", "issue", "SLA", "risk"],
  "summary": "Support intent detected: ticket and SLA values were validated by the local MCP test endpoint.",
  "recommendations": [
    "Group tickets by urgency and SLA risk.",
    "Escalate the highest-risk incidents to senior responders."
  ],
  "data": [
    {
      "ticketId": "INC-1042",
      "owner": "Tier 2 Support",
      "priority": "High",
      "status": "Open",
      "slaRisk": "At risk",
      "customerHealth": "At risk"
    }
  ],
  "metadata": {
    "responseMode": "remote-mcp-test",
    "generatedAt": "2026-10-29T00:00:00Z"
  }
}
```

When the app is triggered through `/api/mcp/execute`, the returned `rawMcpResponse` should reflect the same values in the test payload. Verify:

- `rawMcpResponse.intent`
- `rawMcpResponse.summary`
- `rawMcpResponse.data[0].ticketId`
- `rawMcpResponse.data[0].slaRisk`

If a real MCP server is configured, the app uses that instead; otherwise it keeps the existing dynamic fallback path and still returns valid business data.

## Run the app

```bash
mvn spring-boot:run
```

Then open:

- UI: http://localhost:8080/
- Health check: http://localhost:8080/api/mcp/health

## API

### POST /api/mcp/execute

Request body:

```json
{
  "prompt": "Check support tickets for payment issue and SLA risk.",
  "context": "User wants support analytics."
}
```

Example response structure:

```json
{
  "requestId": "uuid",
  "status": "success",
  "rawMcpResponse": {
    "intent": "support",
    "summary": "Support intent detected: ticket and SLA values were validated by the local MCP test endpoint.",
    "data": [
      {
        "ticketId": "INC-1042",
        "priority": "High",
        "slaRisk": "At risk"
      }
    ]
  },
  "aiModifiedResponse": {
    "status": "ai-processed",
    "summary": "AI-generated summary ...",
    "finalResponse": "AI-generated summary ..."
  },
  "finalResponse": "AI-generated summary ..."
}
```

## Chat history

The app keeps recent request/response pairs in memory and exposes them through:

- `GET /api/chat/history`

## Docker deployment

Build and run with Docker Compose:

```bash
docker compose build
docker compose up
```

The app is exposed on port 8080.

## Notes

- Keep secrets outside source control; use environment variables or a local `.env` file.
- If `OPENAI_API_KEY` is missing, the app returns a clear configuration message.
- The default MCP endpoint is now self-contained and prevents the `Connection refused` warning during local testing.
- The smart fallback code remains unchanged.
