# MCP AI Studio

This project is a real Spring Boot application that exposes a REST API, triggers an MCP-style tool workflow, and then sends the result to OpenAI for AI processing before returning the final answer.

## What this project does

- Accepts a user prompt via REST API
- Runs an MCP-style tool workflow (local fallback or remote MCP endpoint)
- Sends the prompt and MCP payload to OpenAI with tool-calling support where supported
- Returns the AI-modified response to the caller
- Stores recent prompts in an in-memory chat history dashboard
- Shows a Streamlit-like web UI for interactive use

## Important security note

I cannot access or read your secret OpenAI API key.

Instead, this project is configured to read the key from an environment variable named OPENAI_API_KEY. Do not hard-code the key into source files or commit it to Git.

## Prerequisites

- Java 21+
- Maven 3.9+
- An OpenAI API key

## Setup

On Windows PowerShell:

```powershell
$env:OPENAI_API_KEY="your-openai-secret-key"
$env:OPENAI_MODEL="gpt-4o-mini"
mvn spring-boot:run
```

On macOS/Linux:

```bash
export OPENAI_API_KEY="your-openai-secret-key"
export OPENAI_MODEL="gpt-4o-mini"
mvn spring-boot:run
```

## OpenAI configuration

The app reads these values from `src/main/resources/application.properties`:

```properties
openai.api-key=${OPENAI_API_KEY:}
openai.model=${OPENAI_MODEL:gpt-4o-mini}
openai.organization=${OPENAI_ORG:}
```

You may also set:

```bash
export OPENAI_ORG="your-openai-org-id"  # optional
```

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
  "prompt": "Get weather data for Bangalore and Singapore and highlight travel recommendations.",
  "context": "User is planning travel."
}
```

Example successful response:

```json
{
  "requestId": "uuid",
  "status": "success",
  "rawMcpResponse": {
    "tool": "weather_lookup",
    "data": [
      { "city": "Bangalore", "temperature": "28°C" }
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

## MCP behavior

The app has a remote MCP integration point and a built-in fallback. If `mcp.server-url` is blank, it uses a sample tool payload so the app still runs without a real MCP server. When you add a real MCP endpoint, set:

```properties
mcp.server-url=https://your-mcp-server
```

The OpenAI layer then processes that MCP result as part of the AI workflow.

## Chat history

The app keeps the last 10 request/response pairs in memory and exposes them through:

- `GET /api/chat/history`

The UI displays the most recent entries in the left sidebar.

## Docker deployment

Build and run with Docker Compose:

```bash
docker compose build
docker compose up
```

The app is exposed on port 8080.

## Notes

- Do not commit your `.env` or secret values to source control.
- If you do not set `OPENAI_API_KEY`, the app will return a clear error message explaining the missing configuration.
- For OpenAI tool calling, prefer a supported function-calling model such as `gpt-4o-mini`.
